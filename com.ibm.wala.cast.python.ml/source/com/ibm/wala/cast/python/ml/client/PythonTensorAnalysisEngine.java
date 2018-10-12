package com.ibm.wala.cast.python.ml.client;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.lsp.AnalysisError;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ml.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.ml.types.TensorType;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.MethodReference;
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

	private final Map<PointerKey, AnalysisError> errorLog = HashMapFactory.make();
	
	private static Set<PointsToSetVariable> getDataflowSources(Graph<PointsToSetVariable> dataflow) {
		Set<PointsToSetVariable> sources = HashSetFactory.make();
		for(PointsToSetVariable src : dataflow) {
			PointerKey k = src.getPointerKey();
			if (k instanceof LocalPointerKey) {
				LocalPointerKey kk = (LocalPointerKey)k;
				int vn = kk.getValueNumber();
				DefUse du = kk.getNode().getDU();
				SSAInstruction inst = du.getDef(vn);
				if (inst instanceof SSAInvokeInstruction) {
					SSAInvokeInstruction ni = (SSAInvokeInstruction) inst;
					if (ni.getCallSite().getDeclaredTarget().getName().toString().equals("read_data") && ni.getException() != vn) {
						sources.add(src);
					}
				}
			}
		}
		return sources;
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
	
	protected void addBypassLogic(AnalysisOptions options) {
		super.addBypassLogic(options);
		addSummaryBypassLogic(options, "tensorflow.xml");
	}

}
