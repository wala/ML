package com.ibm.wala.cast.python.test;

import static java.util.Collections.emptyList;

import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.client.PythonTurtleAnalysisEngine.EdgeType;
import com.ibm.wala.cast.python.client.PythonTurtleAnalysisEngine.TurtlePath;
import com.ibm.wala.cast.python.client.PythonTurtlePandasMergeAnalysis;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.labeled.LabeledGraph;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class TestPythonTurtlePandaMergeCallGraphShape extends TestPythonTurtleCallGraphShape {

  public TestPythonTurtlePandaMergeCallGraphShape() {
    super(false);
  }

  @Override
  protected PythonAnalysisEngine<LabeledGraph<TurtlePath, EdgeType>> makeEngine(
      List<File> pythonPath, String... name)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<LabeledGraph<TurtlePath, EdgeType>> engine =
        new PythonTurtlePandasMergeAnalysis();
    Set<Module> modules = HashSetFactory.make();
    for (String n : name) {
      modules.add(getScript(n));
    }
    engine.setModuleFiles(modules);
    return engine;
  }

  @Override
  protected PythonAnalysisEngine<LabeledGraph<TurtlePath, EdgeType>> makeEngine(String... name)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    return makeEngine(emptyList(), name);
  }

  public static void main(String[] args)
      throws IllegalArgumentException, ClassHierarchyException, CancelException, IOException {
    TestPythonTurtlePandaMergeCallGraphShape driver =
        new TestPythonTurtlePandaMergeCallGraphShape();
    PythonAnalysisEngine<LabeledGraph<TurtlePath, EdgeType>> E = driver.makeEngine(args);

    SSAPropagationCallGraphBuilder builder =
        (SSAPropagationCallGraphBuilder) E.defaultCallGraphBuilder();
    CallGraph CG = builder.makeCallGraph(E.getOptions(), new NullProgressMonitor());

    Graph<TurtlePath> analysis = E.performAnalysis((SSAPropagationCallGraphBuilder) builder);
  }
}
