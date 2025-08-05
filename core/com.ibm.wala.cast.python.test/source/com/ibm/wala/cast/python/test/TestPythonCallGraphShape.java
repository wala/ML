package com.ibm.wala.cast.python.test;

import static java.util.Collections.emptyList;

import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cast.util.test.TestCallGraphShape;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class TestPythonCallGraphShape extends TestCallGraphShape {

  public TestPythonCallGraphShape() {
    super();
  }

  @Override
  public Collection<CGNode> getNodes(CallGraph CG, String functionIdentifier) {
    if (functionIdentifier.contains(":")) {
      String cls = functionIdentifier.substring(0, functionIdentifier.indexOf(":"));
      String name = functionIdentifier.substring(functionIdentifier.indexOf(":") + 1);
      return CG.getNodes(
          MethodReference.findOrCreate(
              TypeReference.findOrCreate(
                  PythonTypes.pythonLoader, TypeName.string2TypeName("L" + cls)),
              Atom.findOrCreateUnicodeAtom(name),
              AstMethodReference.fnDesc));
    } else {
      return CG.getNodes(
          MethodReference.findOrCreate(
              TypeReference.findOrCreate(
                  PythonTypes.pythonLoader, TypeName.string2TypeName("L" + functionIdentifier)),
              AstMethodReference.fnSelector));
    }
  }

  protected SourceURLModule getScript(String name) throws IOException {
    try {
      File f = new File(name);
      if (f.exists()) {
        return new SourceURLModule(f.toURI().toURL());
      } else {
        URL url = new URL(name);
        return new SourceURLModule(url);
      }
    } catch (MalformedURLException e) {
      return new SourceURLModule(getClass().getClassLoader().getResource(name));
    }
  }

  protected PythonAnalysisEngine<?> createEngine(List<File> pythonPath)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    return new PythonAnalysisEngine<Void>(pythonPath) {
      @Override
      public Void performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
        assert false;
        return null;
      }
    };
  }

  protected PythonAnalysisEngine<?> makeEngine(PythonAnalysisEngine<?> engine, String... name)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    Set<Module> modules = HashSetFactory.make();
    for (String n : name) {
      modules.add(getScript(n));
    }
    assert ! modules.isEmpty() : name;
    engine.setModuleFiles(modules);
    return engine;
  }

  protected PythonAnalysisEngine<?> makeEngine(List<File> pythonPath, String... name)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    return makeEngine(createEngine(pythonPath), name);
  }

  protected PythonAnalysisEngine<?> makeEngine(String... name)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    return makeEngine(createEngine(emptyList()), name);
  }

  protected CallGraph process(List<File> pythonPath, String... name)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    return makeEngine(pythonPath, name).buildDefaultCallGraph();
  }

  protected CallGraph process(String... name)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    return process(emptyList(), name);
  }

  StringBuffer dump(CallGraph CG) {
    StringBuffer sb = new StringBuffer();
    for (CGNode n : CG) {
      sb.append(n.getIR()).append("\n");
    }
    return sb;
  }
}
