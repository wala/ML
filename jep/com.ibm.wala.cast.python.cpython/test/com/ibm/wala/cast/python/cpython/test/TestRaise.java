package com.ibm.wala.cast.python.cpython.test;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.test.TestJythonCallGraphShape;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import org.junit.Test;

public class TestRaise extends TestJythonCallGraphShape {

  protected static final Object[][] assertionsForRaise =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script raise.py"}},
        new Object[] {
          "script raise.py",
          new String[] {
            "script raise.py/e1",
            "script raise.py/e2",
            "script raise.py/e3",
            "$script raise.py/e1/f:trampoline1",
            "$script raise.py/e2/f:trampoline1",
            "$script raise.py/e3/f:trampoline1"
          }
        },
        new Object[] {"$script raise.py/e1/f:trampoline1", new String[] {"script raise.py/e1/f"}},
        new Object[] {"$script raise.py/e2/f:trampoline1", new String[] {"script raise.py/e2/f"}},
        new Object[] {"$script raise.py/e3/f:trampoline1", new String[] {"script raise.py/e3/f"}}
      };

  @Test
  public void testRaise()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<?> engine = this.makeEngine("raise.py");
    PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
    CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

    System.err.println(CG);
    CAstCallGraphUtil.AVOID_DUMP.set(false);
    CAstCallGraphUtil.dumpCG(
        (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
        callGraphBuilder.getPointerAnalysis(),
        CG);

    verifyGraphAssertions(CG, assertionsForRaise);
  }

  protected static final Object[][] assertionsForRaise2 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script raise2.py"}},
        new Object[] {
          "script raise2.py",
          new String[] {
            "script raise2.py/f1",
            "script raise2.py/f2",
            "script raise2.py/f3",
            "$script raise2.py/e1/f:trampoline1"
          }
        },
        new Object[] {"script raise2.py/f1", new String[] {"script raise2.py/e1"}},
        new Object[] {
          "script raise2.py/f2", new String[] {"script raise2.py/e2", "script raise2.py/e3"}
        },
        new Object[] {
          "script raise2.py/f3",
          new String[] {"$script raise2.py/e2/f:trampoline1", "$script raise2.py/e3/f:trampoline1"}
        },
        new Object[] {"$script raise2.py/e1/f:trampoline1", new String[] {"script raise2.py/e1/f"}},
        new Object[] {"$script raise2.py/e2/f:trampoline1", new String[] {"script raise2.py/e2/f"}},
        new Object[] {"$script raise2.py/e3/f:trampoline1", new String[] {"script raise2.py/e3/f"}}
      };

  @Test
  public void testRaise2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<?> engine = this.makeEngine("raise2.py");
    PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
    CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

    System.err.println(CG);
    CAstCallGraphUtil.AVOID_DUMP.set(false);
    CAstCallGraphUtil.dumpCG(
        (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
        callGraphBuilder.getPointerAnalysis(),
        CG);

    verifyGraphAssertions(CG, assertionsForRaise2);
  }

  @Test
  public void testRaise3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<?> engine = this.makeEngine("raise3.py");
    PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
    CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

    System.err.println(CG);
    CAstCallGraphUtil.AVOID_DUMP.set(false);
    CAstCallGraphUtil.dumpCG(
        (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
        callGraphBuilder.getPointerAnalysis(),
        CG);

    // verifyGraphAssertions(CG, assertionsForRaise2);
  }
}
