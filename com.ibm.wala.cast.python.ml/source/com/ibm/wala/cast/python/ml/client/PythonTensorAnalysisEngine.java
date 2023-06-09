package com.ibm.wala.cast.python.ml.client;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.google.common.base.Objects;
import com.ibm.wala.cast.lsp.AnalysisError;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.ml.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.ml.types.TensorType;
import com.ibm.wala.cast.python.ssa.PythonPropertyRead;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.Value;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;

public class PythonTensorAnalysisEngine extends PythonAnalysisEngine<TensorTypeAnalysis> {
	private static final MethodReference conv2d = MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName("Ltensorflow/functions/conv2d")), AstMethodReference.fnSelector);
	
	private static final MethodReference conv3d = MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName("Ltensorflow/functions/conv3d")), AstMethodReference.fnSelector);

	private static final MethodReference reshape = MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName("Ltensorflow/functions/reshape")), AstMethodReference.fnSelector);

	private static final MethodReference placeholder = MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName("Ltensorflow/functions/placeholder")), AstMethodReference.fnSelector);

	private static final MethodReference set_shape = MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName("Ltensorflow/functions/set_shape")), AstMethodReference.fnSelector);

	private static final MethodReference import_tensorflow = MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName("Ltensorflow")), Selector.make(PythonLanguage.Python, "import()Ltensorflow;"));

	private final Map<PointerKey, AnalysisError> errorLog = HashMapFactory.make();
	
	private static Set<PointsToSetVariable> getDataflowSources(Graph<PointsToSetVariable> dataflow) {
		Set<PointsToSetVariable> sources = HashSetFactory.make();
		for(PointsToSetVariable src : dataflow) {
			PointerKey k = src.getPointerKey();

			if (k instanceof LocalPointerKey) {
				LocalPointerKey kk = (LocalPointerKey)k;
				int vn = kk.getValueNumber();
				CGNode node = kk.getNode();
				DefUse du = node.getDU();
				SSAInstruction inst = du.getDef(vn);

				if (inst instanceof SSAAbstractInvokeInstruction) {
					SSAAbstractInvokeInstruction ni = (SSAAbstractInvokeInstruction) inst;

					// A stack of API calls starting from the right-most API from the selection operator, e.g., tf.random.uniform.
					Stack<String> tensorFlowAPIStack = new Stack<>();

					if (!ni.isStatic()) {
						int receiver = ni.getReceiver();
						SSAInstruction receiverDefinition = du.getDef(receiver);

						if (receiverDefinition instanceof PythonPropertyRead) {
							PythonPropertyRead propertyRead = (PythonPropertyRead) receiverDefinition;

							// are we calling a TensorFlow API?
							if (isFromTensorFlow(propertyRead, du)) {
								int memberRef = propertyRead.getMemberRef();
								SSAInstruction memberRefDefinition = du.getDef(memberRef);

								// if the member reference can't be found.
								if (memberRefDefinition == null) {
									// look it up in the IR.
									IR ir = node.getIR();
									Value memberRefValue = ir.getSymbolTable().getValue(memberRef);

									if (memberRefValue.isStringConstant()) {
										// push the API onto the stack.
										tensorFlowAPIStack.push(ir.getSymbolTable().getStringValue(memberRef));

										// If the API module uses a function, the code below gets the module. For example: random.uniform
										Value memberRefValuePrevious = ir.getSymbolTable().getValue(memberRef - 1);
										if (memberRefValuePrevious != null && memberRefValuePrevious.isStringConstant()) {
												tensorFlowAPIStack.push(ir.getSymbolTable().getStringValue(memberRef-1));
										}
									}
								}
							}
						}
					}

					String tensorFlowAPI = null;
					if (!tensorFlowAPIStack.isEmpty())
						tensorFlowAPI = tensorFlowAPIStack.pop();

					// First-level APIs
					if ((ni.getCallSite().getDeclaredTarget().getName().toString().equals("read_data")
							|| Objects.equal(tensorFlowAPI, "ones") || Objects.equal(tensorFlowAPI, "Variable")
							|| Objects.equal(tensorFlowAPI, "zeros") || Objects.equal(tensorFlowAPI, "constant"))
							&& ni.getException() != vn) {
						sources.add(src);
					// Second-level APIs
					} else if (Objects.equal(tensorFlowAPI, "random") && ni.getException() != vn) {
						if (!tensorFlowAPIStack.isEmpty()) {
							tensorFlowAPI = tensorFlowAPIStack.pop();
							if (Objects.equal(tensorFlowAPI, "uniform"))
								sources.add(src);
						}
					}
				}
			}
		}
		return sources;
	}

	/**
	 * True iff the given {@link PythonPropertyRead} corresponds to a TensorFlow API
	 * invocation.
	 *
	 * @param propertyRead The {@link PythonPropertyRead} to check.
	 * @param du           The {@link DefUse} from the corresponding {@link CGNode}.
	 * @return True iff the given {@link PythonPropertyRead} corresponds to a
	 *         TensorFlow API invocation.
	 */
	private static boolean isFromTensorFlow(PythonPropertyRead propertyRead, DefUse du) {
		int objectRef = propertyRead.getObjectRef();
		SSAInstruction objectRefDefinition = du.getDef(objectRef);

		if (objectRefDefinition instanceof SSAInvokeInstruction) {
			SSAInvokeInstruction objectRefInvocInstruction = (SSAInvokeInstruction) objectRefDefinition;
			MethodReference objectRefInvocationDeclaredTarget = objectRefInvocInstruction.getDeclaredTarget();
			return objectRefInvocationDeclaredTarget.equals(import_tensorflow);
		} else if (objectRefDefinition instanceof PythonPropertyRead)
			// it's an import tree. Dig deeper to find the root.
			return isFromTensorFlow((PythonPropertyRead) objectRefDefinition, du);
		return false;
	}

	@FunctionalInterface
	interface SourceCallHandler {
		void handleCall(CGNode src, SSAAbstractInvokeInstruction call);
	}
	
	private void getSourceCalls(MethodReference op, PropagationCallGraphBuilder builder, SourceCallHandler handler) {
		for(CGNode n : builder.getCallGraph()) {
			if (n.getMethod().getReference().equals(op)) {
				for(Iterator<CGNode> srcs = builder.getCallGraph().getPredNodes(n); srcs.hasNext(); ) {
					CGNode src = srcs.next();
					for(Iterator<CallSiteReference> sites = builder.getCallGraph().getPossibleSites(src, n); sites.hasNext(); ) {
						CallSiteReference site = sites.next();
						for(SSAAbstractInvokeInstruction call : src.getIR().getCalls(site)) {
							handler.handleCall(src, call);
						}
					}
				}
			}
		}
	}

	private Map<PointsToSetVariable,TensorType> getShapeSourceCalls(MethodReference op, PropagationCallGraphBuilder builder, int param) {
		Map<PointsToSetVariable,TensorType> targets = HashMapFactory.make();
		getSourceCalls(op, builder, (CGNode src, SSAAbstractInvokeInstruction call) -> {
			if (call.getNumberOfUses() > param) {
			targets.put(
				builder.getPropagationSystem().findOrCreatePointsToSet(builder.getPointerAnalysis().getHeapModel().getPointerKeyForLocal(src, call.getDef())),
				TensorType.shapeArg(src, call.getUse(param)));
			}
		});
		return targets;
	}
	
	private Set<PointsToSetVariable> getKeysDefinedByCall(MethodReference op, PropagationCallGraphBuilder builder) {
		Set<PointsToSetVariable> lvals = HashSetFactory.make();
		getSourceCalls(op, builder, (CGNode src, SSAAbstractInvokeInstruction call) -> {
			lvals.add(builder.getPropagationSystem().findOrCreatePointsToSet(builder.getPointerAnalysis().getHeapModel().getPointerKeyForLocal(src, call.getDef())));
		});
		return lvals;
	}
	
	@Override
	public TensorTypeAnalysis performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
		Graph<PointsToSetVariable> dataflow = SlowSparseNumberedGraph.duplicate(builder.getPropagationSystem().getFlowGraphIncludingImplicitConstraints());

		Set<PointsToSetVariable> sources = getDataflowSources(dataflow);
		
		TensorType mnistData = TensorType.mnistInput();
		Map<PointsToSetVariable, TensorType> init = HashMapFactory.make();
		for(PointsToSetVariable v : sources) {
			init.put(v, mnistData);			
		}

		Map<PointsToSetVariable, TensorType> placeholders = handleShapeSourceOp(builder, dataflow, placeholder, 2);
		System.err.println(placeholders);
		for(Map.Entry<PointsToSetVariable, TensorType> e : placeholders.entrySet()) {
			init.put(e.getKey(), e.getValue());
		}

		Map<PointsToSetVariable, TensorType> setCalls = HashMapFactory.make();
		Map<PointsToSetVariable, TensorType> set_shapes = getShapeSourceCalls(set_shape, builder, 1);		
		for(Map.Entry<PointsToSetVariable, TensorType> x : set_shapes.entrySet()) {
			CGNode setNode = ((LocalPointerKey)x.getKey().getPointerKey()).getNode();
			int defVn = ((LocalPointerKey)x.getKey().getPointerKey()).getValueNumber();
			SSAInstruction read = setNode.getDU().getDef(defVn);
			SSAInstruction call = setNode.getDU().getDef(read.getUse(0));
			PointerKey setKey = builder.getPointerAnalysis().getHeapModel().getPointerKeyForLocal(setNode, call.getUse(0));
			setCalls.put(builder.getPropagationSystem().findOrCreatePointsToSet(setKey), x.getValue());
		}

		Map<PointsToSetVariable, TensorType> shapeOps = HashMapFactory.make();
		shapeOps.putAll(handleShapeSourceOp(builder, dataflow, reshape, 2));
		
		Set<PointsToSetVariable> conv2ds = getKeysDefinedByCall(conv2d, builder);

		Set<PointsToSetVariable> conv3ds = getKeysDefinedByCall(conv3d, builder);
		
		TensorTypeAnalysis tt = new TensorTypeAnalysis(dataflow, init, shapeOps, setCalls, conv2ds, conv3ds, errorLog);
		
		tt.solve(new NullProgressMonitor());
		
		return tt;
	}

	private Map<PointsToSetVariable, TensorType> handleShapeSourceOp(PropagationCallGraphBuilder builder,
			Graph<PointsToSetVariable> dataflow, MethodReference op, int shapeSrcOperand) {
		Map<PointsToSetVariable, TensorType> reshapeTypes = getShapeSourceCalls(op, builder, shapeSrcOperand);			
		for(PointsToSetVariable to : reshapeTypes.keySet()) {
			assert to.getPointerKey() instanceof LocalPointerKey;
			int toVn = ((LocalPointerKey)to.getPointerKey()).getValueNumber();
			CGNode srcNode = ((LocalPointerKey)to.getPointerKey()).getNode();
			int srcVn = srcNode.getDU().getDef(toVn).getUse(1);
			PointerKey from = builder.getPointerAnalysis().getHeapModel().getPointerKeyForLocal(srcNode, srcVn);
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
