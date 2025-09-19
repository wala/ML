package com.ibm.wala.cast.python.cpython.test;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ipa.callgraph.PythonSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.python.test.TestJythonCallGraphShape;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import org.junit.Test;

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
            "script match1.py/sunday",
            "script match1.py/otherDay"
          }
        }
      };

  @Test
  public void testMatch1()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<?> E = makeEngine("match1.py");
    PythonSSAPropagationCallGraphBuilder B = E.defaultCallGraphBuilder();
    CallGraph CG = B.makeCallGraph(B.getOptions());

    System.err.println(CG);
    CAstCallGraphUtil.AVOID_DUMP.set(false);
    CAstCallGraphUtil.dumpCG(
        (SSAContextInterpreter) B.getContextInterpreter(), B.getPointerAnalysis(), CG);

    verifyGraphAssertions(CG, assertionsForMatch1);
  }

  protected static final Object[][] assertionsForMatch2 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script match2.py"}},
        new Object[] {"script match2.py", new String[] {"script match2.py/doit"}},
        new Object[] {
          "script match2.py/doit",
          new String[] {
            "script match2.py/monday",
            "script match2.py/tuesday",
            "script match2.py/wednesday",
            "script match2.py/thursday",
            "script match2.py/friday",
            "script match2.py/saturday",
            "script match2.py/sunday",
            "script match2.py/otherDay"
          }
        }
      };

  @Test
  public void testMatch2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<?> E = makeEngine("match2.py");
    PythonSSAPropagationCallGraphBuilder B = E.defaultCallGraphBuilder();
    CallGraph CG = B.makeCallGraph(B.getOptions());

    System.err.println(CG);
    CAstCallGraphUtil.AVOID_DUMP.set(false);
    CAstCallGraphUtil.dumpCG(
        (SSAContextInterpreter) B.getContextInterpreter(), B.getPointerAnalysis(), CG);

    verifyGraphAssertions(CG, assertionsForMatch2);
  }
  
  protected static final Object[][] assertionsForMatch3 =
	      new Object[][] {
	        new Object[] {ROOT, new String[] {"script match3.py"}},
	        new Object[] {"script match3.py", new String[] {"script match3.py/doit"}},
	        new Object[] {
	          "script match3.py/doit",
	          new String[] {
	            "script match3.py/weekday",
	            "script match3.py/weekend",
	            "script match3.py/otherDay"
	          }
	        }
	      };

	  @Test
	  public void testMatch3()
	      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
	    PythonAnalysisEngine<?> E = makeEngine("match3.py");
	    PythonSSAPropagationCallGraphBuilder B = E.defaultCallGraphBuilder();
	    CallGraph CG = B.makeCallGraph(B.getOptions());

	    System.err.println(CG);
	    CAstCallGraphUtil.AVOID_DUMP.set(false);
	    CAstCallGraphUtil.dumpCG(
	        (SSAContextInterpreter) B.getContextInterpreter(), B.getPointerAnalysis(), CG);

	    verifyGraphAssertions(CG, assertionsForMatch3);
	  }

}
