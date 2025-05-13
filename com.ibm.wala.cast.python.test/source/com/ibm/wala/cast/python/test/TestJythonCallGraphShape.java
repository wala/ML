package com.ibm.wala.cast.python.test;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.util.Util;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import java.io.IOException;

public abstract class TestJythonCallGraphShape extends TestPythonCallGraphShape {

  public static void main(String[] args)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    TestJythonCallGraphShape driver = new TestJythonCallGraphShape() {};

    PythonAnalysisEngine<?> E = driver.makeEngine(Util.getPathFiles(args[1]), args[0]);

    CallGraphBuilder<? super InstanceKey> builder = E.defaultCallGraphBuilder();
    CallGraph CG = builder.makeCallGraph(E.getOptions(), new NullProgressMonitor());

    CAstCallGraphUtil.AVOID_DUMP.set(false);
    CAstCallGraphUtil.dumpCG(
        ((SSAPropagationCallGraphBuilder) builder).getCFAContextInterpreter(),
        builder.getPointerAnalysis(),
        CG);
  }
}
