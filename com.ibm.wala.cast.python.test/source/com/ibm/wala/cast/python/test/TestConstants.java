package com.ibm.wala.cast.python.test;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import java.util.Collection;
import org.junit.Test;

public class TestConstants extends TestPythonCallGraphShape {

  @Test
  public void testFolding()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("ana1.py");
    Collection<CGNode> bananas = getNodes(CG, "script ana1.py/Banana");
    assert bananas.size() == 1;
    CGNode banana = bananas.iterator().next();
    System.err.println(banana.getIR());
  }
}
