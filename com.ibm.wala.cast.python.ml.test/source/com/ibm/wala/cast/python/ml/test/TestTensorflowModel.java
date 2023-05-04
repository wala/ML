package com.ibm.wala.cast.python.ml.test;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;

import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ipa.callgraph.PythonSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.python.ml.analysis.TensorTypeAnalysis;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestTensorflowModel extends TestPythonMLCallGraphShape {

	@Test
	public void testTf1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<TensorTypeAnalysis> E = makeEngine("tf1.py");
		PythonSSAPropagationCallGraphBuilder builder = E.defaultCallGraphBuilder();
		CallGraph CG = builder.makeCallGraph(builder.getOptions());
		
//		CAstCallGraphUtil.AVOID_DUMP = false;
//		CAstCallGraphUtil.dumpCG(((SSAPropagationCallGraphBuilder)builder).getCFAContextInterpreter(), builder.getPointerAnalysis(), CG);

//		System.err.println(CG);
		
		Collection<CGNode> nodes = getNodes(CG, "script tf1.py/model_fn");
		assert ! nodes.isEmpty() : "model_fn should be called";
		check: {
			for(CGNode node : nodes) {
				for(Iterator<CGNode> ns = CG.getPredNodes(node); ns.hasNext(); ) {
					if (ns.next().getMethod().isWalaSynthetic()) {
						break check;
					}
				}
			
				assert false : node + " should have synthetic caller";
			}
		}
	}

}
