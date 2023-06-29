package com.ibm.wala.cast.python.driver;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.loader.PythonLoaderFactory;
import com.ibm.wala.cast.python.util.PythonInterpreter;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceDirectoryTreeModule;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import java.io.File;
import java.io.IOException;
import java.util.Set;

public class Driver {

  static {
    try {
      Class<?> j3 = Class.forName("com.ibm.wala.cast.python.loader.Python3LoaderFactory");
      PythonAnalysisEngine.setLoaderFactory((Class<? extends PythonLoaderFactory>) j3);
      Class<?> i3 = Class.forName("com.ibm.wala.cast.python.util.Python3Interpreter");
      PythonInterpreter.setInterpreter((PythonInterpreter) i3.newInstance());
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      try {
        Class<?> j2 = Class.forName("com.ibm.wala.cast.python.loader.Python2LoaderFactory");
        PythonAnalysisEngine.setLoaderFactory((Class<? extends PythonLoaderFactory>) j2);
        Class<?> i2 = Class.forName("com.ibm.wala.cast.python.util.Python2Interpreter");
        PythonInterpreter.setInterpreter((PythonInterpreter) i2.newInstance());
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e1) {
        assert false : e.getMessage() + ", then " + e1.getMessage();
      }
    }
  }

  protected <T> T runit(PythonAnalysisEngine<T> E, String... args)
      throws IOException, CancelException {
    Set<Module> sources = HashSetFactory.make();
    for (String file : args) {
      File fo = new File(file);
      if (fo.isDirectory()) {
        sources.add(new SourceDirectoryTreeModule(fo, ".py"));
      } else {
        sources.add(new SourceFileModule(fo, file, null));
      }
    }
    E.setModuleFiles(sources);

    CallGraphBuilder<? super InstanceKey> builder = E.defaultCallGraphBuilder();

    CallGraph CG = builder.makeCallGraph(E.getOptions(), new NullProgressMonitor());

    System.err.println(CG);

    @SuppressWarnings("unchecked")
    PointerAnalysis<InstanceKey> PA = (PointerAnalysis<InstanceKey>) builder.getPointerAnalysis();

    CAstCallGraphUtil.AVOID_DUMP = false;
    CAstCallGraphUtil.dumpCG(
        ((SSAPropagationCallGraphBuilder) builder).getCFAContextInterpreter(), PA, CG);

    SDG<InstanceKey> SDG =
        new SDG<InstanceKey>(
            CG,
            PA,
            DataDependenceOptions.NO_EXCEPTIONS,
            ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
    SDG.forEach(
        n -> {
          System.err.println(n);
          if (SDG.getSuccNodeCount(n) > 0) {
            SDG.getSuccNodes(n)
                .forEachRemaining(
                    s -> {
                      System.err.println("  --> " + s);
                    });
          }
        });

    return E.performAnalysis((PropagationCallGraphBuilder) builder);
  }

  public static void main(String... args)
      throws IllegalArgumentException, IOException, CancelException {

    PythonAnalysisEngine<Void> E =
        new PythonAnalysisEngine<Void>() {
          @Override
          public Void performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
            return null;
          }
        };

    new Driver().runit(E, args);
  }
}
