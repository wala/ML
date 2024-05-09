package com.ibm.wala.cast.python.ml.client;

import static com.google.common.collect.Sets.newHashSet;
import static com.ibm.wala.cast.python.ml.types.TensorFlowTypes.DATASET;
import static com.ibm.wala.cast.types.AstMethodReference.fnReference;

import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.lsp.AnalysisError;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ml.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.ml.types.TensorType;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
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
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
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
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class PythonTensorAnalysisEngine extends PythonAnalysisEngine<TensorTypeAnalysis> {

  public PythonTensorAnalysisEngine() {}

  public PythonTensorAnalysisEngine(List<File> pythonPath) {
    super(pythonPath);
  }

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

  private static final MethodReference ENUMERATE =
      MethodReference.findOrCreate(
          TypeReference.findOrCreate(
              PythonTypes.pythonLoader, TypeName.string2TypeName("Lwala/builtin/enumerate")),
          AstMethodReference.fnSelector);

  private static final MethodReference NEXT =
      MethodReference.findOrCreate(
          TypeReference.findOrCreate(
              PythonTypes.pythonLoader, TypeName.string2TypeName("Lwala/builtin/next")),
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
          } else if (ni.getNumberOfUses() > 1) {
            // Get the invoked function from the PA.
            int target = ni.getUse(0);
            PointerKey targetKey =
                pointerAnalysis.getHeapModel().getPointerKeyForLocal(localPointerKeyNode, target);

            for (InstanceKey ik : pointerAnalysis.getPointsToSet(targetKey)) {
              if (ik instanceof ConcreteTypeKey) {
                ConcreteTypeKey ctk = (ConcreteTypeKey) ik;
                IClass type = ctk.getType();
                TypeReference reference = type.getReference();

                if (reference.equals(NEXT.getDeclaringClass())) {
                  // it's a call to `next()`. Look up the call to `iter()`.
                  int iterator = ni.getUse(1);
                  SSAInstruction iteratorDef = du.getDef(iterator);

                  // Let's see if the iterator is over a tensor dataset.
                  if (iteratorDef != null && iteratorDef.getNumberOfUses() > 1) {
                    // Get the argument.
                    int iterArg = iteratorDef.getUse(1);
                    processInstructionInterprocedurally(
                        iteratorDef, iterArg, localPointerKeyNode, src, sources, pointerAnalysis);
                  } else
                    // Use the original instruction. NOTE: We can only do this because `iter()` is
                    // currently just passing-through its argument.
                    processInstructionInterprocedurally(
                        ni, iterator, localPointerKeyNode, src, sources, pointerAnalysis);
                }
              }
            }
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
              || def instanceof PythonPropertyRead
              || def instanceof PythonInvokeInstruction) {
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
   * @param du The {@link DefUse} corresponding to the given {@link SSAInstruction}.
   * @param node The {@link CGNode} containing the given {@link SSAInstruction}.
   * @param src The {@link PointsToSetVariable} under question as to whether it should be considered
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
    return processInstruction(
        instruction, du, node, src, sources, callGraph, pointerAnalysis, newHashSet());
  }

  /**
   * Processes the given {@link SSAInstruction} to decide if the given {@link PointsToSetVariable}
   * is added to the given {@link Set} of {@link PointsToSetVariable}s as tensor dataflow sources.
   *
   * @param instruction The {@link SSAInstruction} to process.
   * @param du The {@link DefUse} corresponding to the given {@link SSAInstruction}.
   * @param node The {@link CGNode} containing the given {@link SSAInstruction}.
   * @param src The {@link PointsToSetVariable} under question as to whether it should be considered
   *     a tensor dataflow source.
   * @param sources The {@link Set} of tensor dataflow sources.
   * @param callGraph The {@link CallGraph} containing the given {@link SSAInstruction}.
   * @param pointerAnalysis The {@link PointerAnalysis} corresponding to the given {@link
   *     CallGraph}.
   * @param seen A {@link Set} of previously processed {@link SSAInstruction}.
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
      PointerAnalysis<InstanceKey> pointerAnalysis,
      Set<SSAInstruction> seen) {
    if (seen.contains(instruction))
      logger.fine(() -> "Skipping instruction: " + instruction + ". We've seen it before.");
    else {
      logger.fine(() -> "Processing instruction: " + instruction + ".");
      seen.add(instruction);

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
            return processInstruction(
                def, du, node, src, sources, callGraph, pointerAnalysis, seen);
        }
      }
    }

    return false;
  }

  /**
   * Similar to processInstruction but does so using the given {@link PointerAnalysis}.
   *
   * @param instruction The {@link SSAInstruction} to be processed.
   * @param use The use in the {@link Instruction} to analyze.
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
            "Using interprocedural analysis to find potential tensor definition for use: "
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

        if (reference.equals(DATASET) && isDatasetTensorElement(src, use, node, pointerAnalysis)) {
          sources.add(src);
          logger.info("Added dataflow source from tensor dataset: " + src + ".");
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns true iff the given {@link PointsToSetVariable} refers to a tensor dataset element of
   * the dataset defined by the given value number in the given {@link CGNode}.
   *
   * @param variable The {@link PointsToSetVariable} to consider.
   * @param val The value in the given {@link CGNode} representing the tensor dataset.
   * @param node The {@link CGNode} containing the given {@link PointsToSetVariable} and value.
   * @param pointerAnalysis The {@link PointerAnalysis} that includes points-to information for the
   *     given {@link CGNode}.
   * @return True iff src refers to a tensor dataset element defined by the dataset represented by
   *     val in node.
   */
  private static boolean isDatasetTensorElement(
      PointsToSetVariable variable,
      int val,
      CGNode node,
      PointerAnalysis<InstanceKey> pointerAnalysis) {
    SSAInstruction def = node.getDU().getDef(val);

    if (def instanceof PythonInvokeInstruction) {
      PythonInvokeInstruction invokeInstruction = (PythonInvokeInstruction) def;

      // Check whether we are calling enumerate(), as that returns a tuple.
      // Get the invoked function.
      int invocationUse = invokeInstruction.getUse(0);

      PointerKey invocationUsePointerKey =
          pointerAnalysis.getHeapModel().getPointerKeyForLocal(node, invocationUse);

      for (InstanceKey functionInstance : pointerAnalysis.getPointsToSet(invocationUsePointerKey)) {
        if (functionInstance instanceof ConcreteTypeKey) {
          ConcreteTypeKey typeKey = (ConcreteTypeKey) functionInstance;
          IClass type = typeKey.getType();
          TypeReference typeReference = type.getReference();

          if (typeReference.equals(ENUMERATE.getDeclaringClass())) {
            // it's a call to enumerate(), where the returned value is an iterator over
            // tuples. Each tuple consists of the enumeration number and the dataset
            // element. Check that we are not looking at the enumeration number.

            PythonPropertyRead srcDef =
                (PythonPropertyRead)
                    node.getDU()
                        .getDef(((LocalPointerKey) variable.getPointerKey()).getValueNumber());

            // What does the member reference point to?
            PointerKey memberRefPointerKey =
                pointerAnalysis.getHeapModel().getPointerKeyForLocal(node, srcDef.getMemberRef());

            for (InstanceKey memberInstance : pointerAnalysis.getPointsToSet(memberRefPointerKey)) {
              ConstantKey<?> constant = (ConstantKey<?>) memberInstance;
              Object value = constant.getValue();

              // if it's the first tuple element.
              if (value.equals(0)) {
                // Now that we know it's the first tuple element, we now need to know whether it's
                // the first tuple, i.e., the one returned by enumerate.
                // To do that, we examine the object being referenced on the RHS.

                SSAInstruction objRefDef = node.getDU().getDef(srcDef.getObjectRef());

                // If the object being read is that of the dataset, we know that this is the first
                // tuple read of the result of enumerate() on the dataset.
                if (objRefDef instanceof PythonPropertyRead
                    && ((PythonPropertyRead) objRefDef).getObjectRef() == val) return false;
              }
            }
          }
        }
      }
    }

    return true;
  }

  /**
   * True iff the given {@link SSAInstruction} constitutes individual elements.
   *
   * @param instruction The {@link SSAInstruction} in question.
   * @param du The {@link DefUse} for the containing {@link CGNode}.
   * @return True iff the definition of the given {@link EachElementGetInstruction} is non-scalar.
   */
  private static boolean definitionIsNonScalar(SSAInstruction instruction, DefUse du) {
    int def = instruction.getDef();
    logger.fine("Processing definition: " + def + " of instruction: " + instruction + ".");

    int numberOfUses = du.getNumberOfUses(def);
    logger.fine(
        "Definition: "
            + def
            + " of instruction: "
            + instruction
            + " has "
            + numberOfUses
            + " uses.");

    for (Iterator<SSAInstruction> uses = du.getUses(def); uses.hasNext(); ) {
      SSAInstruction useInstruction = uses.next();
      logger.fine("Processing use: " + useInstruction + ".");

      if (useInstruction instanceof PythonPropertyRead) {
        PythonPropertyRead read = (PythonPropertyRead) useInstruction;
        logger.fine("Found property read use: " + read + ".");

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

    // Don't handle shape source operations for now to workaround
    // https://github.com/wala/ML/issues/195.

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
