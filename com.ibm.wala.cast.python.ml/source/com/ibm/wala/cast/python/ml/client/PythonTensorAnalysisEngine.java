package com.ibm.wala.cast.python.ml.client;

import static com.ibm.wala.cast.python.ml.types.TensorFlowTypes.DATASET;
import static com.ibm.wala.cast.types.AstMethodReference.fnReference;

import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.lsp.AnalysisError;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ml.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.ml.types.TensorType;
import com.ibm.wala.cast.python.ssa.PythonPropertyRead;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.intset.OrdinalSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class PythonTensorAnalysisEngine extends PythonAnalysisEngine<TensorTypeAnalysis> {

  /** A "fake" function name in the summaries that indicates that an API produces a new tensor. */
  private static final String TENSOR_GENERATOR_SYNTHETIC_FUNCTION_NAME = "read_data";

  /**
   * A "fake" function name in the summaries that indicates that an API produces a tensor iterable.
   */
  private static final String TENSOR_ITERABLE_SYNTHETIC_FUNCTION_NAME = "read_dataset";

  private static final Logger logger = Logger.getLogger(PythonTensorAnalysisEngine.class.getName());

  private static final MethodReference conv2d =
      MethodReference.findOrCreate(
          TypeReference.findOrCreate(
              PythonTypes.pythonLoader, TypeName.string2TypeName("Ltensorflow/functions/conv2d")),
          AstMethodReference.fnSelector);

  private static final MethodReference conv3d =
      MethodReference.findOrCreate(
          TypeReference.findOrCreate(
              PythonTypes.pythonLoader, TypeName.string2TypeName("Ltensorflow/functions/conv3d")),
          AstMethodReference.fnSelector);

  private static final MethodReference reshape =
      MethodReference.findOrCreate(
          TypeReference.findOrCreate(
              PythonTypes.pythonLoader, TypeName.string2TypeName("Ltensorflow/functions/reshape")),
          AstMethodReference.fnSelector);

  private static final MethodReference placeholder =
      MethodReference.findOrCreate(
          TypeReference.findOrCreate(
              PythonTypes.pythonLoader,
              TypeName.string2TypeName("Ltensorflow/functions/placeholder")),
          AstMethodReference.fnSelector);

  private static final MethodReference set_shape =
      MethodReference.findOrCreate(
          TypeReference.findOrCreate(
              PythonTypes.pythonLoader,
              TypeName.string2TypeName("Ltensorflow/functions/set_shape")),
          AstMethodReference.fnSelector);

  private final Map<PointerKey, AnalysisError> errorLog = HashMapFactory.make();

  private static Set<PointsToSetVariable> getDataflowSources(
      Graph<PointsToSetVariable> dataflow,
      CallGraph callGraph,
      PointerAnalysis<InstanceKey> pointerAnalysis) {
    Set<PointsToSetVariable> sources = HashSetFactory.make();
    for (PointsToSetVariable src : dataflow) {
      PointerKey k = src.getPointerKey();

      if (k instanceof LocalPointerKey) {
        LocalPointerKey kk = (LocalPointerKey) k;
        int vn = kk.getValueNumber();
        CGNode localPointerKeyNode = kk.getNode();
        DefUse du = localPointerKeyNode.getDU();
        SSAInstruction inst = du.getDef(vn);

        if (inst instanceof SSAAbstractInvokeInstruction) {
          // We potentially have a function call that generates a tensor.
          SSAAbstractInvokeInstruction ni = (SSAAbstractInvokeInstruction) inst;

          if (ni.getCallSite()
                  .getDeclaredTarget()
                  .getName()
                  .toString()
                  .equals(TENSOR_GENERATOR_SYNTHETIC_FUNCTION_NAME)
              && ni.getException() != vn) {
            sources.add(src);
            logger.info("Added dataflow source from tensor generator: " + src + ".");
          }
        } else if (inst instanceof EachElementGetInstruction) {
          // We are potentially pulling a tensor out of a tensor iterable.
          EachElementGetInstruction eachElementGetInstruction = (EachElementGetInstruction) inst;

          // Don't add the source if the container has elements in it. In that case, we want to add
          // the individual elements themselves as sources instead.
          if (definitionIsNonScalar(eachElementGetInstruction, du))
            logger.info(
                "Definition of instruction: "
                    + eachElementGetInstruction
                    + " is non-scalar. Skipping...");
          else {
            logger.info(
                "Definition of instruction: "
                    + eachElementGetInstruction
                    + " is scalar. Processing...");

            // Find the potential tensor iterable definition.
            processInstruction(
                eachElementGetInstruction,
                du,
                localPointerKeyNode,
                src,
                sources,
                callGraph,
                pointerAnalysis);
          }
        } else if (inst instanceof PythonPropertyRead) {
          // We are potentially pulling a tensor out of a non-scalar tensor iterable.
          PythonPropertyRead propertyRead = (PythonPropertyRead) inst;

          // Find the potential tensor iterable definition.
          int objectRef = propertyRead.getObjectRef();
          SSAInstruction def = du.getDef(objectRef);

          if (def == null) {
            // definition is unavailable from the local DefUse. Use interprocedural analysis using
            // the PA.
            processInstructionInterprocedurally(
                propertyRead, objectRef, localPointerKeyNode, src, sources, pointerAnalysis);
          } else if (def instanceof EachElementGetInstruction
              || def instanceof PythonPropertyRead) {
            processInstruction(
                def, du, localPointerKeyNode, src, sources, callGraph, pointerAnalysis);
          }
        }
      }
    }
    return sources;
  }

  /**
   * Processes the given {@link SSAInstruction} to decide if the given {@link PointsToSetVariable}
   * is added to the given {@link Set} of {@link PointsToSetVariable}s as tensor dataflow sources.
   *
   * @param instruction The {@link SSAInstruction} to process.
   * @param du The {@link DefUse} corresponding to the siven {@link SSAInstruction}.
   * @param node The {@link CGNode} containing the given {@link SSAInstruction}.
   * @param src The {@link PointsToSetVariable} under question as to whether it shoudl be considered
   *     a tensor dataflow source.
   * @param sources The {@link Set} of tensor dataflow sources.
   * @param callGraph The {@link CallGraph} containing the given {@link SSAInstruction}.
   * @param pointerAnalysis The {@link PointerAnalysis} corresponding to the given {@link
   *     CallGraph}.
   * @return True iff the given {@link PointsToSetVariable} was added to the given {@link Set} of
   *     {@link PointsToSetVariable} dataflow sources.
   */
  private static boolean processInstruction(
      SSAInstruction instruction,
      DefUse du,
      CGNode node,
      PointsToSetVariable src,
      Set<PointsToSetVariable> sources,
      CallGraph callGraph,
      PointerAnalysis<InstanceKey> pointerAnalysis) {
    logger.fine(() -> "Processing instruction: " + instruction + ".");

    if (instruction != null && instruction.getNumberOfUses() > 0) {
      int use = instruction.getUse(0);
      SSAInstruction def = du.getDef(use);

      // First try intraprocedural analysis.
      if (definesTensorIterable(def, node, callGraph, pointerAnalysis)) {
        sources.add(src);
        logger.info("Added dataflow source from tensor iterable: " + src + ".");
        return true;
      } else {
        // Use interprocedural analysis using the PA.
        boolean added =
            processInstructionInterprocedurally(
                instruction, use, node, src, sources, pointerAnalysis);

        if (added) return true;
        else
          // keep going up.
          return processInstruction(def, du, node, src, sources, callGraph, pointerAnalysis);
      }
    }

    return false;
  }

  /**
   * Similar to processInstruction but does so using the given {@link PointerAnalysis}.
   *
   * @param instruction The {@link SSAInstruction} to be processed.
   * @param use The {@link DefUse} corresponding to the given {@link SSAInstruction}.
   * @param node The {@link CGNode} containing the given {@link SSAInstruction}.
   * @param src The {@link PointsToSetVariable} being decided upon whether it should be considered
   *     as a tensor dataflow source.
   * @param sources The {@link Set} of all tensor dataflow sources, i.e., {@link
   *     PointsToSetVariable}s.
   * @param pointerAnalysis The {@link PointerAnalysis} built from the given {@link CGNode}'s {@link
   *     CallGraph}.
   * @return True iff the given {@link PointsToSetVariable} was added to the given set of tensor
   *     dataflow sources, i.e., the given {@link Set} of {@link PointsToSetVariable}s.
   */
  private static boolean processInstructionInterprocedurally(
      SSAInstruction instruction,
      int use,
      CGNode node,
      PointsToSetVariable src,
      Set<PointsToSetVariable> sources,
      PointerAnalysis<InstanceKey> pointerAnalysis) {
    logger.info(
        () ->
            "Using interprocedural analysis to find potential tensor iterable definition for use: "
                + use
                + " of instruction: "
                + instruction
                + ".");

    // Look up the use in the pointer analysis to see if it points to a dataset.
    PointerKey usePointerKey = pointerAnalysis.getHeapModel().getPointerKeyForLocal(node, use);

    for (InstanceKey ik : pointerAnalysis.getPointsToSet(usePointerKey)) {
      if (ik instanceof AllocationSiteInNode) {
        AllocationSiteInNode asin = (AllocationSiteInNode) ik;
        IClass concreteType = asin.getConcreteType();
        TypeReference reference = concreteType.getReference();

        if (reference.equals(DATASET)) {
          sources.add(src);
          logger.info("Added dataflow source from tensor dataset: " + src + ".");
          return true;
        }
      }
    }

    return false;
  }

  /**
   * True iff the given {@link EachElementGetInstruction} constitutes individual elements.
   *
   * @param eachElementGetInstruction The {@link EachElementGetInstruction} in question.
   * @param du The {@link DefUse} for the containing {@link CGNode}.
   * @return True iff the definition of the given {@link EachElementGetInstruction} is non-scalar.
   */
  private static boolean definitionIsNonScalar(
      EachElementGetInstruction eachElementGetInstruction, DefUse du) {
    int def = eachElementGetInstruction.getDef();
    logger.info(
        "Processing definition: " + def + " of instruction: " + eachElementGetInstruction + ".");

    int numberOfUses = du.getNumberOfUses(def);
    logger.info(
        "Definition: "
            + def
            + " of instruction: "
            + eachElementGetInstruction
            + " has "
            + numberOfUses
            + " uses.");

    for (Iterator<SSAInstruction> uses = du.getUses(def); uses.hasNext(); ) {
      SSAInstruction instruction = uses.next();
      logger.info("Processing use: " + instruction + ".");

      if (instruction instanceof PythonPropertyRead) {
        PythonPropertyRead read = (PythonPropertyRead) instruction;
        logger.info("Found property read use: " + read + ".");

        // if the definition appears on the LHS of the read.
        if (read.getObjectRef() == def) return true;
      }
    }
    return false;
  }

  /**
   * Returns true iff the given {@link SSAInstruction} defines an iterable of tensors.
   *
   * @param instruction The {@link SSAInstruction} in question.
   * @param node The {@link CGNode} of the function containing the given {@link SSAInstruction}.
   * @param callGraph The {@link CallGraph} that includes a node corresponding to the given {@link
   *     SSAInstruction}.
   * @param pointerAnalysis The {@link PointerAnalysis} built from the given {@link CallGraph}.
   * @return True iff the given {@link SSAInstruction} defines an iterable over tensors.
   */
  private static boolean definesTensorIterable(
      SSAInstruction instruction,
      CGNode node,
      CallGraph callGraph,
      PointerAnalysis<InstanceKey> pointerAnalysis) {
    if (instruction instanceof SSAAbstractInvokeInstruction) {
      SSAAbstractInvokeInstruction invocationInstruction =
          (SSAAbstractInvokeInstruction) instruction;

      if (invocationInstruction.getNumberOfUses() > 0) {
        // What function are we calling?
        int use = invocationInstruction.getUse(0);
        PointerKey pointerKeyForLocal =
            pointerAnalysis.getHeapModel().getPointerKeyForLocal(node, use);
        OrdinalSet<InstanceKey> pointsToSet = pointerAnalysis.getPointsToSet(pointerKeyForLocal);

        for (InstanceKey ik : pointsToSet) {
          if (ik instanceof AllocationSiteInNode) {
            AllocationSiteInNode asin = (AllocationSiteInNode) ik;
            IClass concreteType = asin.getConcreteType();
            TypeReference reference = concreteType.getReference();
            MethodReference methodReference = fnReference(reference);

            // Get the nodes this method calls.
            Set<CGNode> iterableNodes = callGraph.getNodes(methodReference);

            for (CGNode itNode : iterableNodes)
              for (Iterator<CGNode> succNodes = callGraph.getSuccNodes(itNode);
                  succNodes.hasNext(); ) {
                CGNode callee = succNodes.next();
                IMethod calledMethod = callee.getMethod();

                // Does this method call the synthetic "marker?"
                if (calledMethod
                    .getName()
                    .toString()
                    .equals(TENSOR_ITERABLE_SYNTHETIC_FUNCTION_NAME)) {
                  return true;
                }
              }
          }
        }
      }
    }
    return false;
  }

  @FunctionalInterface
  interface SourceCallHandler {
    void handleCall(CGNode src, SSAAbstractInvokeInstruction call);
  }

  private void getSourceCalls(
      MethodReference op, PropagationCallGraphBuilder builder, SourceCallHandler handler) {
    for (CGNode n : builder.getCallGraph()) {
      if (n.getMethod().getReference().equals(op)) {
        for (Iterator<CGNode> srcs = builder.getCallGraph().getPredNodes(n); srcs.hasNext(); ) {
          CGNode src = srcs.next();
          for (Iterator<CallSiteReference> sites = builder.getCallGraph().getPossibleSites(src, n);
              sites.hasNext(); ) {
            CallSiteReference site = sites.next();
            for (SSAAbstractInvokeInstruction call : src.getIR().getCalls(site)) {
              handler.handleCall(src, call);
            }
          }
        }
      }
    }
  }

  private Map<PointsToSetVariable, TensorType> getShapeSourceCalls(
      MethodReference op, PropagationCallGraphBuilder builder, int param) {
    Map<PointsToSetVariable, TensorType> targets = HashMapFactory.make();
    getSourceCalls(
        op,
        builder,
        (CGNode src, SSAAbstractInvokeInstruction call) -> {
          if (call.getNumberOfUses() > param) {
            targets.put(
                builder
                    .getPropagationSystem()
                    .findOrCreatePointsToSet(
                        builder
                            .getPointerAnalysis()
                            .getHeapModel()
                            .getPointerKeyForLocal(src, call.getDef())),
                TensorType.shapeArg(src, call.getUse(param)));
          }
        });
    return targets;
  }

  private Set<PointsToSetVariable> getKeysDefinedByCall(
      MethodReference op, PropagationCallGraphBuilder builder) {
    Set<PointsToSetVariable> lvals = HashSetFactory.make();
    getSourceCalls(
        op,
        builder,
        (CGNode src, SSAAbstractInvokeInstruction call) -> {
          lvals.add(
              builder
                  .getPropagationSystem()
                  .findOrCreatePointsToSet(
                      builder
                          .getPointerAnalysis()
                          .getHeapModel()
                          .getPointerKeyForLocal(src, call.getDef())));
        });
    return lvals;
  }

  @Override
  public TensorTypeAnalysis performAnalysis(PropagationCallGraphBuilder builder)
      throws CancelException {
    Graph<PointsToSetVariable> dataflow =
        SlowSparseNumberedGraph.duplicate(
            builder.getPropagationSystem().getFlowGraphIncludingImplicitConstraints());

    Set<PointsToSetVariable> sources =
        getDataflowSources(dataflow, builder.getCallGraph(), builder.getPointerAnalysis());

    TensorType mnistData = TensorType.mnistInput();
    Map<PointsToSetVariable, TensorType> init = HashMapFactory.make();
    for (PointsToSetVariable v : sources) {
      init.put(v, mnistData);
    }

    Map<PointsToSetVariable, TensorType> placeholders =
        handleShapeSourceOp(builder, dataflow, placeholder, 2);
    System.err.println(placeholders);
    for (Map.Entry<PointsToSetVariable, TensorType> e : placeholders.entrySet()) {
      init.put(e.getKey(), e.getValue());
    }

    Map<PointsToSetVariable, TensorType> setCalls = HashMapFactory.make();
    Map<PointsToSetVariable, TensorType> set_shapes = getShapeSourceCalls(set_shape, builder, 1);
    for (Map.Entry<PointsToSetVariable, TensorType> x : set_shapes.entrySet()) {
      CGNode setNode = ((LocalPointerKey) x.getKey().getPointerKey()).getNode();
      int defVn = ((LocalPointerKey) x.getKey().getPointerKey()).getValueNumber();
      SSAInstruction read = setNode.getDU().getDef(defVn);
      SSAInstruction call = setNode.getDU().getDef(read.getUse(0));
      PointerKey setKey =
          builder
              .getPointerAnalysis()
              .getHeapModel()
              .getPointerKeyForLocal(setNode, call.getUse(0));
      setCalls.put(builder.getPropagationSystem().findOrCreatePointsToSet(setKey), x.getValue());
    }

    Map<PointsToSetVariable, TensorType> shapeOps = HashMapFactory.make();
    shapeOps.putAll(handleShapeSourceOp(builder, dataflow, reshape, 2));

    Set<PointsToSetVariable> conv2ds = getKeysDefinedByCall(conv2d, builder);

    Set<PointsToSetVariable> conv3ds = getKeysDefinedByCall(conv3d, builder);

    TensorTypeAnalysis tt =
        new TensorTypeAnalysis(dataflow, init, shapeOps, setCalls, conv2ds, conv3ds, errorLog);

    tt.solve(new NullProgressMonitor());

    return tt;
  }

  private Map<PointsToSetVariable, TensorType> handleShapeSourceOp(
      PropagationCallGraphBuilder builder,
      Graph<PointsToSetVariable> dataflow,
      MethodReference op,
      int shapeSrcOperand) {
    Map<PointsToSetVariable, TensorType> reshapeTypes =
        getShapeSourceCalls(op, builder, shapeSrcOperand);
    for (PointsToSetVariable to : reshapeTypes.keySet()) {
      assert to.getPointerKey() instanceof LocalPointerKey;
      int toVn = ((LocalPointerKey) to.getPointerKey()).getValueNumber();
      CGNode srcNode = ((LocalPointerKey) to.getPointerKey()).getNode();
      int srcVn = srcNode.getDU().getDef(toVn).getUse(1);
      PointerKey from =
          builder.getPointerAnalysis().getHeapModel().getPointerKeyForLocal(srcNode, srcVn);
      dataflow.addEdge(builder.getPropagationSystem().findOrCreatePointsToSet(from), to);
    }
    return reshapeTypes;
  }

  public Map<PointerKey, AnalysisError> getErrors() {
    return errorLog;
  }

  protected void addBypassLogic(IClassHierarchy cha, AnalysisOptions options) {
    super.addBypassLogic(cha, options);
    addSummaryBypassLogic(options, "tensorflow.xml");
  }
}
