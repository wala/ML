package com.ibm.wala.cast.python.cpython.test;

import com.ibm.wala.cast.ipa.modref.AstModRef;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.test.TestPythonLibraryCallGraphShape;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import org.junit.Test;

public class TestPandasExample extends TestPythonLibraryCallGraphShape {

  @Test
  public void testExample()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<?> engine = this.makeEngine("/Users/dolby/Documents/python_example.py");
    PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
    CallGraph CG = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());
    SDG<InstanceKey> sdg =
        new SDG<>(
            CG,
            callGraphBuilder.getPointerAnalysis(),
            AstModRef.make(),
            DataDependenceOptions.FULL,
            ControlDependenceOptions.NONE);
    System.err.println(sdg);
  }
}
