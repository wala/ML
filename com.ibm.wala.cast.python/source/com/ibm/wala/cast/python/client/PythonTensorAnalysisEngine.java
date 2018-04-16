package com.ibm.wala.cast.python.client;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.python.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.python.types.TensorType;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.ReturnValueKey;
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

public class PythonTensorAnalysisEngine extends PythonAnalysisEngine {
	private static final MethodReference reshape = MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName("Ltensorflow/functions/reshape")), AstMethodReference.fnSelector);

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

	private Map<PointsToSetVariable,TensorType> getReshapeTypes(PropagationCallGraphBuilder builder) {
		Map<PointsToSetVariable,TensorType> targets = HashMapFactory.make();
		for(CGNode n : builder.getCallGraph()) {
			if (n.getMethod().getReference().equals(reshape)) {
				for(Iterator<CGNode> srcs = builder.getCallGraph().getPredNodes(n); srcs.hasNext(); ) {
					CGNode src = srcs.next();
					for(Iterator<CallSiteReference> sites = builder.getCallGraph().getPossibleSites(src, n); sites.hasNext(); ) {
						CallSiteReference site = sites.next();
						for(SSAAbstractInvokeInstruction call : src.getIR().getCalls(site)) {
							targets.put(
								builder.getPropagationSystem().findOrCreatePointsToSet(builder.getPointerAnalysis().getHeapModel().getPointerKeyForReturnValue(n)),
								TensorType.reshapeArg(src, call.getUse(2)));
						}
					}
				}
			}
		}
		return targets;
	}
	
	@Override
	public TensorTypeAnalysis performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
		Graph<PointsToSetVariable> dataflow = SlowSparseNumberedGraph.duplicate(builder.getPropagationSystem().getFlowGraphIncludingImplicitConstraints());

		Set<PointsToSetVariable> sources = getDataflowSources(dataflow);
		System.err.println(sources);
		
		TensorType mnistData = TensorType.mnistInput();
		Map<PointsToSetVariable, TensorType> init = HashMapFactory.make();
		for(PointsToSetVariable v : sources) {
			init.put(v, mnistData);
		}
		
		Map<PointsToSetVariable, TensorType> reshapeTypes = getReshapeTypes(builder);			
		for(PointsToSetVariable to : reshapeTypes.keySet()) {
			assert to.getPointerKey() instanceof ReturnValueKey;
			PointerKey from = builder.getPointerAnalysis().getHeapModel().getPointerKeyForLocal(((ReturnValueKey)to.getPointerKey()).getNode(), 2);
			dataflow.addEdge(builder.getPropagationSystem().findOrCreatePointsToSet(from), to);
		}
		
		TensorTypeAnalysis tt = new TensorTypeAnalysis(dataflow, init, reshapeTypes);
		
		tt.solve(new NullProgressMonitor());
		
		return tt;
	}
}
