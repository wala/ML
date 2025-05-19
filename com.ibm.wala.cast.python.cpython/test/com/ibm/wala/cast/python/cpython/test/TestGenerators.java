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

public class TestGenerators extends TestJythonCallGraphShape {

	  protected static final Object[][] assertionsForGen1 =
			  new Object[][] {
		  new Object[] {ROOT, new String[] {"script gen1.py"}},
		  new Object[] {
				  "script gen1.py",
				  new String[] {"script gen1.py/f1/lambda1", "script gen1.py/f2/lambda2", 
						  "script gen1.py/f3/lambda3", "script gen1.py/f1", 
						  "script gen1.py/f2", "script gen1.py/f3"}
		  }
	  };


	  @Test
	  public void testGen1()
	      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		  PythonAnalysisEngine<?> engine = this.makeEngine("gen1.py");
		  PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		  CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

		  CAstCallGraphUtil.AVOID_DUMP.set(false);
		  CAstCallGraphUtil.dumpCG(
				  (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
				  callGraphBuilder.getPointerAnalysis(),
				  CG);

		  verifyGraphAssertions(CG, assertionsForGen1);
	  }

}
