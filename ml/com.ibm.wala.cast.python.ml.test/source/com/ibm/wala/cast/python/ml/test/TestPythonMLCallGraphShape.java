package com.ibm.wala.cast.python.ml.test;

import static java.util.Collections.emptyList;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ml.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.ml.analysis.TensorVariable;
import com.ibm.wala.cast.python.ml.client.PythonTensorAnalysisEngine;
import com.ibm.wala.cast.python.test.TestJythonCallGraphShape;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.python.util.Util;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public abstract class TestPythonMLCallGraphShape extends TestJythonCallGraphShape {

  private static final Logger LOGGER = Logger.getLogger(TestPythonMLCallGraphShape.class.getName());

  @FunctionalInterface
  protected interface CheckTensorOps {
    void check(PropagationCallGraphBuilder cgBuilder, CallGraph CG, TensorTypeAnalysis result);
  }

  @Override
  protected PythonTensorAnalysisEngine makeEngine(List<File> pythonPath, String... name)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonTensorAnalysisEngine engine = new PythonTensorAnalysisEngine(pythonPath);
    Set<Module> modules = HashSetFactory.make();
    for (String n : name) {
      modules.add(getScript(n));
    }
    engine.setModuleFiles(modules);
    return engine;
  }

  @Override
  protected PythonAnalysisEngine<TensorTypeAnalysis> makeEngine(String... name)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    return makeEngine(emptyList(), name);
  }

  protected void checkTensorOp(
      PropagationCallGraphBuilder cgBuilder,
      CallGraph CG,
      TensorTypeAnalysis result,
      String fn,
      String in,
      String out) {
    boolean found = false;
    Set<CGNode> nodes =
        CG.getNodes(
            MethodReference.findOrCreate(
                TypeReference.findOrCreate(PythonTypes.pythonLoader, "Ltensorflow/functions/" + fn),
                AstMethodReference.fnSelector));
    assert nodes.size() > 0;
    for (CGNode node : nodes) {
      for (Iterator<CGNode> callers = CG.getPredNodes(node); callers.hasNext(); ) {
        CGNode caller = callers.next();
        for (Iterator<CallSiteReference> sites = CG.getPossibleSites(caller, node);
            sites.hasNext(); ) {
          for (SSAAbstractInvokeInstruction call : caller.getIR().getCalls(sites.next())) {
            TensorVariable orig =
                result.getOut(
                    cgBuilder
                        .getPropagationSystem()
                        .findOrCreatePointsToSet(
                            cgBuilder.getPointerKeyForLocal(caller, call.getUse(1))));
            boolean thisOne = (in == null || in.equals(orig.getTypes().toString()));

            TensorVariable reshaped =
                result.getOut(
                    cgBuilder
                        .getPropagationSystem()
                        .findOrCreatePointsToSet(
                            cgBuilder.getPointerKeyForLocal(caller, call.getDef())));
            thisOne &= (out == null || out.equals(reshaped.getTypes().toString()));

            if (thisOne) {
              found = true;
              break;
            }
          }
        }
      }
    }
    assert found;
  }

  protected void checkTensorOps(String url, CheckTensorOps check)
      throws IllegalArgumentException, CancelException, IOException, URISyntaxException {
    PythonTensorAnalysisEngine e = new PythonTensorAnalysisEngine();
    e.setModuleFiles(Collections.singleton(new SourceURLModule(new URI(url).toURL())));
    PropagationCallGraphBuilder cgBuilder =
        (PropagationCallGraphBuilder) e.defaultCallGraphBuilder();
    CallGraph CG = cgBuilder.makeCallGraph(cgBuilder.getOptions());
    TensorTypeAnalysis result = e.performAnalysis(cgBuilder);
    LOGGER.info("Tensor analysis result: " + result);
    check.check(cgBuilder, CG, result);
  }

  public static void main(String[] args)
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    TestPythonMLCallGraphShape driver = new TestPythonMLCallGraphShape() {};

    PythonAnalysisEngine<?> E = driver.makeEngine(Util.getPathFiles(args[1]), args[0]);

    CallGraphBuilder<? super InstanceKey> builder = E.defaultCallGraphBuilder();
    CallGraph CG = builder.makeCallGraph(E.getOptions(), new NullProgressMonitor());

    CAstCallGraphUtil.AVOID_DUMP.set(false);
    CAstCallGraphUtil.dumpCG(
        ((SSAPropagationCallGraphBuilder) builder).getCFAContextInterpreter(),
        E.getPointerAnalysis(),
        CG);
  }
}
