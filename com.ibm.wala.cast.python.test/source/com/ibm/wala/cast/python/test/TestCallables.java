package com.ibm.wala.cast.python.test;

import static org.junit.Assert.assertTrue;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ipa.callgraph.PythonSSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import java.util.Iterator;
import org.junit.Test;

public class TestCallables extends TestPythonCallGraphShape {

  @Test
  public void testCallables()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    final String[] testFileNames = {"callables.py", "callables2.py"};

    for (String fileName : testFileNames) {
      PythonAnalysisEngine<?> E = makeEngine(fileName);
      PythonSSAPropagationCallGraphBuilder B = E.defaultCallGraphBuilder();
      CallGraph CG = B.makeCallGraph(B.getOptions());

      CAstCallGraphUtil.AVOID_DUMP = false;
      CAstCallGraphUtil.dumpCG(
          (SSAContextInterpreter) B.getContextInterpreter(), B.getPointerAnalysis(), CG);

      boolean found = false;

      for (CGNode node : CG) {
        if (node.getMethod()
            .getDeclaringClass()
            .getName()
            .toString()
            .equals("Lscript " + fileName)) {
          for (Iterator<CGNode> it = CG.getSuccNodes(node); it.hasNext(); ) {
            CGNode callee = it.next();
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
