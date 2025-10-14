package com.ibm.wala.cast.python.test;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import java.util.logging.Logger;
import org.junit.Test;

public class TestSource extends TestJythonCallGraphShape {

  private static final Logger LOGGER = Logger.getLogger(TestSource.class.getName());

  @Test
  public void testSource1()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("src1.py");
    CG.forEach(
        (n) -> {
          LOGGER.fine("Node IR: " + n.getIR());
        });
    // verifyGraphAssertions(CG, assertionsCalls1);
  }
}
