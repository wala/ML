package com.ibm.wala.cast.python.test;

import static java.util.Collections.emptyList;

import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.client.PythonTurtleAnalysisEngine;
import com.ibm.wala.cast.python.client.PythonTurtleAnalysisEngine.EdgeType;
import com.ibm.wala.cast.python.client.PythonTurtleAnalysisEngine.TurtlePath;
import com.ibm.wala.cast.python.client.PythonTurtleLibraryAnalysisEngine;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.ReverseIterator;
import com.ibm.wala.util.graph.labeled.LabeledGraph;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;

public class TestPythonTurtleCallGraphShape extends TestPythonCallGraphShape {
  private final boolean isLibrary;

  public TestPythonTurtleCallGraphShape(boolean isLibrary) {
    this.isLibrary = isLibrary;
  }

  @Override
  protected PythonAnalysisEngine<LabeledGraph<TurtlePath, EdgeType>> makeEngine(
      List<File> pythonPath, String... name)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<LabeledGraph<TurtlePath, EdgeType>> engine =
        isLibrary ? new PythonTurtleLibraryAnalysisEngine() : new PythonTurtleAnalysisEngine();
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
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    TestPythonTurtleCallGraphShape driver =
        new TestPythonTurtleCallGraphShape(Boolean.getBoolean("analyzeTurtleLibrary")) {};

    for (String main : args) {
      PythonAnalysisEngine<LabeledGraph<TurtlePath, PythonTurtleAnalysisEngine.EdgeType>> E =
          driver.makeEngine(main);

      CallGraphBuilder<? super InstanceKey> builder = E.defaultCallGraphBuilder();
      CallGraph CG = builder.makeCallGraph(E.getOptions(), new NullProgressMonitor());

      LabeledGraph<TurtlePath, PythonTurtleAnalysisEngine.EdgeType> analysis =
          E.performAnalysis((SSAPropagationCallGraphBuilder) builder);

      JSONArray stuff = PythonTurtleAnalysisEngine.graphToJSON(analysis);

      for (CGNode n : CG) {
        System.err.println(n.getIR());
      }
      System.err.println(CG);
      System.err.println(stuff);

      List<String> pat =
          Arrays.asList("tensorflow", "examples", "tutorials", "mnist", "**", "next_batch");
      analysis.forEach(
          (turtle) -> {
            if (PythonTurtleAnalysisEngine.match(
                ReverseIterator.reverse(turtle.path().iterator()), pat.iterator())) {
              System.err.println(turtle.position() + ": " + turtle.value());
            }
          });
    }
  }
}
