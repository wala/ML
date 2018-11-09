package com.ibm.wala.cast.python.test;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestFor extends TestPythonCallGraphShape {

	 protected static final Object[][] assertionsFor1 = new Object[][] {
		    new Object[] { ROOT, new String[] { "script for1.py" } },
		    new Object[] {
		        "script for1.py",
		        new String[] { "script for1.py/f1", "script for1.py/f2", "script for1.py/f3" } }
	 };

	 @Test
	public void testFor1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("for1.py");
		verifyGraphAssertions(CG, assertionsFor1);
	}

	 protected static final Object[][] assertionsFor2 = new Object[][] {
		    new Object[] { ROOT, new String[] { "script for2.py" } },
		    new Object[] {
			        "script for2.py",
			        new String[] { "script for2.py/doit" } },
		    new Object[] {
		        "script for2.py/doit",
		        new String[] { "script for2.py/f1", "script for2.py/f2", "script for2.py/f3" } }
	 };
	
	@Test
	public void testFor2() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("for2.py");
		verifyGraphAssertions(CG, assertionsFor2);
	}

	 protected static final Object[][] assertionsFor3 = new Object[][] {
		    new Object[] { ROOT, new String[] { "script for3.py" } },
		    new Object[] {
		        "script for3.py",
		        new String[] { "script for3.py/f1", "script for3.py/f2", "script for3.py/f3" } }
	 };
	 
	@Test
	public void testFor3() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("for3.py");
		verifyGraphAssertions(CG, assertionsFor3);
	}
	
	protected static final Object[][] assertionsComp1 = new Object[][] {
		    new Object[] { ROOT, new String[] { "script comp1.py" } },
		    new Object[] {
		        "script comp1.py/comprehension1",
		        new String[] { "script comp1.py/f1", "script comp1.py/f2", "script comp1.py/f3" } },
		    new Object[] {
			    "script comp1.py/comprehension3",
			    new String[] { "script comp1.py/f1/lambda1", "script comp1.py/f2/lambda1", "script comp1.py/f3/lambda1" } }
	 };

	 @Test
	public void testComp1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		 PythonAnalysisEngine<?> e = new PythonAnalysisEngine<Void>() {
			 @Override
			 public Void performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
				 assert false;
				 return null;
			 }
		 };
		 e.setModuleFiles(Collections.singleton(getScript("comp1.py")));
		 PropagationCallGraphBuilder cgBuilder = (PropagationCallGraphBuilder) e.defaultCallGraphBuilder();
		 CallGraph CG = cgBuilder.makeCallGraph(cgBuilder.getOptions());
		 System.err.println(CG);
		 //CAstCallGraphUtil.AVOID_DUMP = false;
		 //CAstCallGraphUtil.dumpCG((SSAContextInterpreter)cgBuilder.getContextInterpreter(), cgBuilder.getPointerAnalysis(), CG);
		 verifyGraphAssertions(CG, assertionsComp1);
	}

	@Test
	public void testComp2() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("comp2.py");
		System.err.println(CG);
	}

	protected static final Object[][] assertionsComp3 = new Object[][] {
	    new Object[] { ROOT, new String[] { "script comp3.py" } },
	    new Object[] {
	        "script comp3.py/comprehension1",
	        new String[] { "script comp3.py/g1", "script comp3.py/g2", 
	        			   "script comp3.py/f1", "script comp3.py/f2", "script comp3.py/f3",
	        			   "script comp3.py/g2/lambda1"} },
	    new Object[] {
	    	"script comp3.py/g2/lambda1",
	    	new String[] { "script comp3.py/f1", "script comp3.py/f2", "script comp3.py/f3" } },
	    new Object[] {
		    "script comp3.py/comprehension3",
		    new String[] { "script comp3.py/f1/lambda1", "script comp3.py/f2/lambda1", "script comp3.py/f3/lambda1" } }
 };

 @Test
	public void testComp3() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("comp3.py");
		System.err.println(CG);
		 verifyGraphAssertions(CG, assertionsComp3);
	}

}
