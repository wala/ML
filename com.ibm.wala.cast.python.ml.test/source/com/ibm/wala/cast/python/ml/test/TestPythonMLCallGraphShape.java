package com.ibm.wala.cast.python.ml.test;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ml.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.ml.analysis.TensorVariable;
import com.ibm.wala.cast.python.ml.client.PythonTensorAnalysisEngine;
import com.ibm.wala.cast.python.test.TestPythonCallGraphShape;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public abstract class TestPythonMLCallGraphShape extends TestPythonCallGraphShape {
	
	@FunctionalInterface
		protected
		interface CheckTensorOps {
			void check(PropagationCallGraphBuilder cgBuilder, CallGraph CG, TensorTypeAnalysis result);
		}
	
	protected CallGraph process(String name) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<TensorTypeAnalysis> engine = new PythonTensorAnalysisEngine();
		engine.setModuleFiles(Collections.singleton(getScript(name)));
		return engine.buildDefaultCallGraph();
	}
	
	protected void checkTensorOp(PropagationCallGraphBuilder cgBuilder, CallGraph CG, TensorTypeAnalysis result, String fn, String in, String out) {
		boolean found = false;
		Set<CGNode> nodes = CG.getNodes(MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, "Ltensorflow/functions/" + fn), AstMethodReference.fnSelector));
		assert nodes.size() > 0;
		for(CGNode node : nodes) {
			for(Iterator<CGNode> callers = CG.getPredNodes(node); callers.hasNext(); ) {
				CGNode caller = callers.next();
				for(Iterator<CallSiteReference> sites = CG.getPossibleSites(caller, node); sites.hasNext(); ) {
					for(SSAAbstractInvokeInstruction call : caller.getIR().getCalls(sites.next())) {
						TensorVariable orig = result.getOut(cgBuilder.getPropagationSystem().findOrCreatePointsToSet(cgBuilder.getPointerKeyForLocal(caller, call.getUse(1))));
						boolean thisOne = (in == null || in.equals(orig.getTypes().toString()));
						
						TensorVariable reshaped = result.getOut(cgBuilder.getPropagationSystem().findOrCreatePointsToSet(cgBuilder.getPointerKeyForLocal(caller, call.getDef())));
						thisOne &= (out == null || out.equals(reshaped.getTypes().toString()));
						
						if (thisOne) {
							found = true;
							break;
						}
					}
				}
			}
		}
		assert found;
	}

	protected void checkTensorOps(String url, CheckTensorOps check)
			throws IllegalArgumentException, CancelException, IOException {
				PythonTensorAnalysisEngine e = new PythonTensorAnalysisEngine();
				e.setModuleFiles(Collections.singleton(new SourceURLModule(new URL(url))));
				PropagationCallGraphBuilder cgBuilder = (PropagationCallGraphBuilder) e.defaultCallGraphBuilder();
				CallGraph CG = cgBuilder.getCallGraph();		
				TensorTypeAnalysis result = e.performAnalysis(cgBuilder);
				System.err.println(result);
				check.check(cgBuilder, CG, result);
			}
}
