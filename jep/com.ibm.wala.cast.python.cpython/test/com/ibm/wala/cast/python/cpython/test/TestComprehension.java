package com.ibm.wala.cast.python.cpython.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.test.TestJythonCallGraphShape;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestComprehension extends TestJythonCallGraphShape {

	protected static final Object[][] assertionsComp1 = new Object[][] {
			new Object[] { ROOT, new String[] { "script comp1.py" } },
			new Object[] { "script comp1.py",
					new String[] { "script comp1.py/f1", "script comp1.py/f2", "script comp1.py/f3",
							"script comp1.py/f1/lambda1", "script comp1.py/f2/lambda2",
							"script comp1.py/f3/lambda3", } } };

	@Test
	public void testComp1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<?> engine = this.makeEngine("comp1.py");
		PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

		/*
		 * CAstCallGraphUtil.AVOID_DUMP.set(false); CAstCallGraphUtil.dumpCG(
		 * (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
		 * callGraphBuilder.getPointerAnalysis(), CG);
		 */
		System.err.println(CG);
		verifyGraphAssertions(CG, assertionsComp1);
	}

	protected static final Object[][] assertionsComp2 = new Object[][] {
			new Object[] { ROOT, new String[] { "script comp2.py" } } };

	@Test
	public void testComp2() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<?> engine = this.makeEngine("comp2.py");
		PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

		/*
		 * CAstCallGraphUtil.AVOID_DUMP.set(false); CAstCallGraphUtil.dumpCG(
		 * (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
		 * callGraphBuilder.getPointerAnalysis(), CG);
		 */
		System.err.println(CG);
		verifyGraphAssertions(CG, assertionsComp2);

	}

	protected static final Object[][] assertionsComp3 = new Object[][] {
			new Object[] { ROOT, new String[] { "script comp3.py" } },
			new Object[] { "script comp3.py",
					new String[] { "script comp3.py/f1", "script comp3.py/f2", "script comp3.py/f3",
							"script comp3.py/f1/lambda1", "script comp3.py/f2/lambda2", "script comp3.py/f3/lambda3",
							"script comp3.py/g1", "script comp3.py/g2", "script comp3.py/g2/lambda4", } } };

	@Test
	public void testComp3() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<?> engine = this.makeEngine("comp3.py");
		PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

		/*
		 * CAstCallGraphUtil.AVOID_DUMP.set(false); CAstCallGraphUtil.dumpCG(
		 * (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
		 * callGraphBuilder.getPointerAnalysis(), CG);
		 */

		System.err.println(CG);
		verifyGraphAssertions(CG, assertionsComp3);
	}

	protected static final Object[][] assertionsComp4 = new Object[][] {
			new Object[] { ROOT, new String[] { "script comp4.py" } } };

	@Test
	public void testComp4() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<?> engine = this.makeEngine("comp4.py");
		PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

		/*
		 * CAstCallGraphUtil.AVOID_DUMP.set(false); CAstCallGraphUtil.dumpCG(
		 * (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
		 * callGraphBuilder.getPointerAnalysis(), CG);
		 */
		System.err.println(CG);
		verifyGraphAssertions(CG, assertionsComp4);

	}

	protected static final Object[][] assertionsComp5 = new Object[][] {
			new Object[] { ROOT, new String[] { "script comp5.py" } },
			new Object[] { "script comp5.py", new String[] { "script comp5.py/lambda1" } } };

	@Test
	public void testComp5() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<?> engine = this.makeEngine("comp5.py");
		PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

		/*
		 * CAstCallGraphUtil.AVOID_DUMP.set(false); CAstCallGraphUtil.dumpCG(
		 * (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
		 * callGraphBuilder.getPointerAnalysis(), CG);
		 */
		System.err.println(CG);
		verifyGraphAssertions(CG, assertionsComp5);

	}

	protected static final Object[][] assertionsComp6 = new Object[][] {
			new Object[] { ROOT, new String[] { "script comp6.py" } },
			new Object[] { "script comp6.py", new String[] { "script comp6.py/mf", "script comp6.py/mf/lambda1" } } };

	@Test
	public void testComp6() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<?> engine = this.makeEngine("comp6.py");
		PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

		/*
		 * CAstCallGraphUtil.AVOID_DUMP.set(false); CAstCallGraphUtil.dumpCG(
		 * (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
		 * callGraphBuilder.getPointerAnalysis(), CG);
		 */
		System.err.println(CG);
		verifyGraphAssertions(CG, assertionsComp6);

	}

}
