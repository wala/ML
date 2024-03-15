package com.ibm.wala.cast.python.test;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import java.io.IOException;
import org.junit.Test;

public class TestMulti extends TestPythonCallGraphShape {

  protected static final Object[][] assertionsCalls1 =
      new Object[][] {new Object[] {ROOT, new String[] {"script calls1.py", "script calls2.py"}}};

  @Test
  public void testCalls1()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("calls1.py", "calls2.py");
    verifyGraphAssertions(CG, assertionsCalls1);
  }

  protected static final Object[][] assertionsMulti1 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script multi1.py", "script multi2.py"}},
        new Object[] {"script multi1.py", new String[] {"script multi2.py/silly"}},
        new Object[] {"script multi2.py/silly", new String[] {"script multi2.py/silly/inner"}},
      };

  @Test
  public void testMulti1()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<?> engine = makeEngine("multi2.py", "multi1.py");
    PropagationCallGraphBuilder builder =
        (PropagationCallGraphBuilder) engine.defaultCallGraphBuilder();
    CallGraph CG = builder.makeCallGraph(engine.getOptions(), new NullProgressMonitor());
    CAstCallGraphUtil.AVOID_DUMP = false;
    CAstCallGraphUtil.dumpCG(
        (SSAContextInterpreter) builder.getContextInterpreter(), builder.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsMulti1);
  }

  protected static final Object[][] assertionsMulti2 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script multi1.py", "script multi2.py"}},
        // TODO: Add the following code once https://github.com/wala/ML/issues/168 is fixed:
        // new Object[] {"script multi1.py", new String[] {"script multi2.py/silly"}},
        // TODO: Add the following code once https://github.com/wala/ML/issues/168 is fixed:
        // new Object[] {"script multi2.py/silly", new String[] {"script multi2.py/silly/inner"}},
      };

  @Test
  public void testMulti2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<?> engine = makeEngine("multi1.py", "multi2.py");
    PropagationCallGraphBuilder builder =
        (PropagationCallGraphBuilder) engine.defaultCallGraphBuilder();
    CallGraph CG = builder.makeCallGraph(engine.getOptions(), new NullProgressMonitor());
    CAstCallGraphUtil.AVOID_DUMP = false;
    CAstCallGraphUtil.dumpCG(
        (SSAContextInterpreter) builder.getContextInterpreter(), builder.getPointerAnalysis(), CG);
    verifyGraphAssertions(CG, assertionsMulti2);
  }
}
