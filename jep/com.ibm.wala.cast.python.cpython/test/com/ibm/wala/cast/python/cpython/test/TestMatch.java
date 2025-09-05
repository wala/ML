package com.ibm.wala.cast.python.cpython.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ipa.callgraph.PythonSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.python.test.TestJythonCallGraphShape;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestMatch extends TestJythonCallGraphShape {
	  protected static final Object[][] assertionsForMatch1 =
		      new Object[][] {
		        new Object[] {ROOT, new String[] {"script match1.py"}},
		        new Object[] {
		          "script match1.py",
		          new String[] {
				            "script match1.py/monday",
				            "script match1.py/tuesday",
				            "script match1.py/wednesday",
				            "script match1.py/thursday",
				            "script match1.py/friday",
				            "script match1.py/saturday",
				            "script match1.py/sunday"}}};

	  @Test
	  public void testMatch1()
	      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
	    PythonAnalysisEngine<?> E = makeEngine("match1.py");
	    PythonSSAPropagationCallGraphBuilder B = E.defaultCallGraphBuilder();
	    CallGraph CG = B.makeCallGraph(B.getOptions());
	    verifyGraphAssertions(CG, assertionsForMatch1);
	  }

}
