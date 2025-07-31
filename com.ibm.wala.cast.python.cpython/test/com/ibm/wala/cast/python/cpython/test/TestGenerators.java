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

		  System.err.println(CG);
		  CAstCallGraphUtil.AVOID_DUMP.set(false);
		  CAstCallGraphUtil.dumpCG(
				  (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
				  callGraphBuilder.getPointerAnalysis(),
				  CG);

		  verifyGraphAssertions(CG, assertionsForGen1);
	  }

	  protected static final Object[][] assertionsForGen2 =
			  new Object[][] {
		  new Object[] {ROOT, new String[] {"script gen2.py"}},
		  new Object[] {
				  "script gen2.py",
				  new String[] {"script gen2.py/f1/lambda1", "script gen2.py/f2/lambda2", 
						  "script gen2.py/f3/lambda3", "script gen2.py/f1", 
						  "script gen2.py/f2", "script gen2.py/f3", "script gen2.py/gen"}
		  }
	  };

	  @Test
	  public void testGen2()
	      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		  PythonAnalysisEngine<?> engine = this.makeEngine("gen2.py");
		  PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		  CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

		  System.err.println(CG);
		  CAstCallGraphUtil.AVOID_DUMP.set(false);
		  CAstCallGraphUtil.dumpCG(
				  (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
				  callGraphBuilder.getPointerAnalysis(),
				  CG);

		  verifyGraphAssertions(CG, assertionsForGen2);
	  }

	  protected static final Object[][] assertionsForGen3 =
			  new Object[][] {
		  new Object[] {ROOT, new String[] {"script gen3.py"}},
		  new Object[] {
				  "script gen3.py",
				  new String[] {"script gen3.py/f1/lambda1", "script gen3.py/f2/lambda2", 
						  "script gen3.py/f3/lambda3", "script gen3.py/makeGenerator"}
		  },
		  new Object[] {
				  "script gen3.py/makeGenerator",
				  new String[] {"script gen3.py/f1", 
									  "script gen3.py/f2", "script gen3.py/f3"}
		  }
	  };


	  @Test
	  public void testGen3()
	      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		  PythonAnalysisEngine<?> engine = this.makeEngine("gen3.py");
		  PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		  CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

		  System.err.println(CG);
		  CAstCallGraphUtil.AVOID_DUMP.set(false);
		  CAstCallGraphUtil.dumpCG(
				  (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
				  callGraphBuilder.getPointerAnalysis(),
				  CG);

		  verifyGraphAssertions(CG, assertionsForGen3);
	  }

	  protected static final Object[][] assertionsForGen4 =
			  new Object[][] {
		  new Object[] {ROOT, new String[] {"script gen4.py"}},
		  new Object[] {
				  "script gen4.py/gen",
				  new String[] { "script gen4.py/gen1" }
		  },
		  new Object[] {
				  "script gen4.py",
				  new String[] {"script gen4.py/f1/lambda1", "script gen4.py/f2/lambda2", 
						  "script gen4.py/f3/lambda3", "script gen4.py/f1", 
						  "script gen4.py/f2", "script gen4.py/f3", "script gen4.py/gen"}
		  }
	  };

	  @Test
	  public void testGen4()
	      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		  PythonAnalysisEngine<?> engine = this.makeEngine("gen4.py");
		  PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		  CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

		  System.err.println(CG);
		  CAstCallGraphUtil.AVOID_DUMP.set(false);
		  CAstCallGraphUtil.dumpCG(
				  (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
				  callGraphBuilder.getPointerAnalysis(),
				  CG);

		  verifyGraphAssertions(CG, assertionsForGen4);
	  }

	  protected static final Object[][] assertionsForGen5 =
			  new Object[][] {
		  new Object[] {ROOT, new String[] {"script gen5.py"}},
		  new Object[] {
				  "script gen5.py/gen",
				  new String[] { "script gen5.py/gen1" }
		  },
		  new Object[] {
				  "script gen5.py",
				  new String[] {"script gen5.py/gen", "script gen5.py/p1", "script gen5.py/p2"}
		  },
		  new Object[] {
				  "script gen5.py/p2",
				  new String[] {"script gen5.py/f1/lambda1", "script gen5.py/f2/lambda2", 
						  "script gen5.py/f3/lambda3", "!script gen5.py/f1", 
						  "!script gen5.py/f2", "!script gen5.py/f3", "!script gen5.py/gen"}
		  },
		  new Object[] {
				  "script gen5.py/p1",
				  new String[] {"script gen5.py/f1", 
						  "script gen5.py/f2", "script gen5.py/f3",
						  "!script gen5.py/f1/lambda1", "!script gen5.py/f2/lambda2", 
						  "!script gen5.py/f3/lambda3", "!script gen5.py/gen"}
		  }
	  };

	  @Test
	  public void testGen5()
	      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		  PythonAnalysisEngine<?> engine = this.makeEngine("gen5.py");
		  PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
		  CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

		  System.err.println(CG);
		  CAstCallGraphUtil.AVOID_DUMP.set(false);
		  CAstCallGraphUtil.dumpCG(
				  (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
				  callGraphBuilder.getPointerAnalysis(),
				  CG);

		  verifyGraphAssertions(CG, assertionsForGen5);
	  }

}
