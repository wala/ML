package com.ibm.wala.cast.python.test;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import org.junit.Test;

public class TestCollections extends TestJythonCallGraphShape {

  @Test
  public void testCollections1()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("collections.py");
    System.err.println(CG);
    // verifyGraphAssertions(CG, assertionsCalls1);
  }
}
