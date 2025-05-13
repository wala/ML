package com.ibm.wala.cast.python.test;

import static org.junit.Assert.assertTrue;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;
import org.junit.Test;

public class TestCallables extends TestJythonCallGraphShape {

  private static Logger logger = Logger.getLogger(TestCallables.class.getName());

  @Test
  public void testCallables()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    final String[] testFileNames = {
      "callables.py",
      "callables2.py",
      "callables3.py",
      "callables4.py",
      "callables5.py",
      "callables6.py"
    };

    for (String fileName : testFileNames) {
      CallGraph CG = process(fileName);
      boolean found = false;

      for (CGNode node : CG) {
        if (node.getMethod()
            .getDeclaringClass()
            .getName()
            .toString()
            .equals("Lscript " + fileName)) {

          for (Iterator<CGNode> it = CG.getSuccNodes(node); it.hasNext(); ) {
            CGNode callee = it.next();

            logger.info("Found callee: " + callee.getMethod().getSignature());

            if (callee
                .getMethod()
                .getDeclaringClass()
                .getName()
                .toString()
                .equals("L$script " + fileName + "/C/__call__")) found = true;
          }
        }
      }

      assertTrue("Expecting to find __call__ method trampoline in: " + fileName + ".", found);
    }
  }
}
