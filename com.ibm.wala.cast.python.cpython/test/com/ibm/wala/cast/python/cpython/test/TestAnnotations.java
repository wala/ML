package com.ibm.wala.cast.python.cpython.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.test.TestJythonCallGraphShape;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestAnnotations extends TestJythonCallGraphShape {
		 
	@Test
	public void testAnnotations3() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<?> engine = this.makeEngine("annotations3.py");
		PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());
	
		CAstCallGraphUtil.AVOID_DUMP.set(false); CAstCallGraphUtil.dumpCG(
			(SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
			callGraphBuilder.getPointerAnalysis(), CG);
		
		System.err.println(CG);
	    //verifyGraphAssertions(CG, assertionsWalrus1);
	}
	
	@Test
	public void testAnnotations4() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<?> engine = this.makeEngine("annotations4.py");
		PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());
		
		CAstCallGraphUtil.AVOID_DUMP.set(false); CAstCallGraphUtil.dumpCG(
			(SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
			callGraphBuilder.getPointerAnalysis(), CG);
		
		System.err.println(CG);
	    //verifyGraphAssertions(CG, assertionsWalrus1);
	}

	@Test
	public void testAnnotations5() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<?> engine = this.makeEngine("annotations5.py");
		PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());
		
		CAstCallGraphUtil.AVOID_DUMP.set(false); CAstCallGraphUtil.dumpCG(
			(SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
			callGraphBuilder.getPointerAnalysis(), CG);
		
		System.err.println(CG);
	    //verifyGraphAssertions(CG, assertionsWalrus1);
	}
}
