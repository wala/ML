package com.ibm.wala.cast.python.test;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import org.junit.Test;

public class TestSource extends TestPythonCallGraphShape {

  @Test
  public void testSource1()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("src1.py");
    CG.forEach(
        (n) -> {
          System.err.println(n.getIR());
        });
    // verifyGraphAssertions(CG, assertionsCalls1);
  }
}
