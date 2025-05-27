package com.ibm.wala.cast.python.test;

import static com.ibm.wala.cast.python.util.Util.addPytestEntrypoints;
import static java.util.Collections.singleton;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;
import org.junit.Test;

public class TestCalls extends TestPythonCallGraphShape {

  private static final Logger LOGGER = Logger.getLogger(TestCalls.class.getName());

  protected static final Object[][] assertionsCalls1 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script calls1.py"}},
        new Object[] {
          "script calls1.py",
          new String[] {
            "script calls1.py/Foo",
            "script calls1.py/foo",
            "$script calls1.py/Foo/foo:trampoline3",
            "script calls1.py/id",
            "script calls1.py/nothing"
          }
        },
        new Object[] {
          "$script calls1.py/Foo/foo:trampoline3", new String[] {"script calls1.py/Foo/foo"}
        },
        new Object[] {"script calls1.py/call", new String[] {"script calls1.py/id"}},
        new Object[] {"script calls1.py/Foo/foo", new String[] {"script calls1.py/id"}},
        new Object[] {"script calls1.py/foo", new String[] {"script calls1.py/call"}}
      };

  @Test
  public void testCalls1()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("calls1.py");
    verifyGraphAssertions(CG, assertionsCalls1);
  }

  protected static final Object[][] assertionsCalls2 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script calls2.py"}},
        new Object[] {
          "script calls2.py",
          new String[] {
            "script calls2.py/Foo/foo",
            "script calls2.py/foo",
            "$script calls2.py/Foo/foo:trampoline3"
          }
        },
        new Object[] {
          "$script calls2.py/Foo/foo:trampoline3", new String[] {"script calls2.py/Foo/foo"}
        },
        new Object[] {"script calls2.py/call", new String[] {"script calls2.py/id"}},
        new Object[] {"script calls2.py/foo", new String[] {"script calls2.py/call"}}
      };

  @Test
  public void testCalls2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("calls2.py");
    verifyGraphAssertions(CG, assertionsCalls2);
  }

  protected static final Object[][] assertionsCalls3 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script calls3.py"}},
        new Object[] {
          "script calls3.py",
          new String[] {"script calls3.py/nothing", "script calls3.py/id", "script calls3.py/foo"}
        },
        new Object[] {"script calls3.py/call", new String[] {"script calls3.py/id"}},
        new Object[] {"script calls3.py/foo", new String[] {"script calls3.py/call"}}
      };

  @Test
  public void testCalls3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("calls3.py");
    verifyGraphAssertions(CG, assertionsCalls3);
  }

  protected static final Object[][] assertionsCalls4 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script calls4.py"}},
        new Object[] {
          "script calls4.py",
          new String[] {"script calls4.py/bad", "script calls4.py/id", "script calls4.py/foo"}
        }
      };

  @Test
  public void testCalls4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("calls4.py");
    verifyGraphAssertions(CG, assertionsCalls4);
  }

  protected static final Object[][] assertionsCalls5 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script calls5.py"}},
        new Object[] {
          "script calls5.py",
          new String[] {
            "script calls5.py/Foo", "$script calls5.py/Foo/foo:trampoline3", "script calls5.py/bad"
          }
        },
        new Object[] {
          "$script calls5.py/Foo/foo:trampoline3", new String[] {"script calls5.py/Foo/foo"}
        },
        new Object[] {"script calls5.py/Foo/foo", new String[] {"script calls5.py/id"}}
      };

  @Test
  public void testCalls5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("calls5.py");
    verifyGraphAssertions(CG, assertionsCalls5);
  }

  protected static final Object[][] assertionsCalls6 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script calls6.py"}},
        new Object[] {
          "script calls6.py",
          new String[] {
            "script calls6.py/Foo", "$script calls6.py/Foo/foo:trampoline3", "script calls6.py/bad"
          }
        },
        new Object[] {
          "$script calls6.py/Foo/foo:trampoline3", new String[] {"script calls6.py/Foo/foo"}
        },
        new Object[] {"script calls6.py/Foo/foo", new String[] {"script calls6.py/id"}}
      };

  @Test
  public void testCalls6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<?> e =
        new PythonAnalysisEngine<Void>() {
          @Override
          public Void performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
            assert false;
            return null;
          }
        };
    e.setModuleFiles(Collections.singleton(getScript("calls6.py")));
    PropagationCallGraphBuilder cgBuilder =
        (PropagationCallGraphBuilder) e.defaultCallGraphBuilder();
    CallGraph CG = cgBuilder.makeCallGraph(cgBuilder.getOptions());
    CAstCallGraphUtil.AVOID_DUMP.set(false);
    CAstCallGraphUtil.dumpCG(
        (SSAContextInterpreter) cgBuilder.getContextInterpreter(),
        cgBuilder.getPointerAnalysis(),
        CG);
    verifyGraphAssertions(CG, assertionsCalls6);
  }

  @Test
  public void testCalls8()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("calls8.py");
    System.err.println(CG);
    CG.forEach(
        (n) -> {
          System.err.println(n.getIR());
        });
    // verifyGraphAssertions(CG, assertionsCalls6);
  }

  @Test
  public void testCalls7()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<?> e =
        new PythonAnalysisEngine<Void>() {
          @Override
          public Void performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
            assert false;
            return null;
          }
        };
    e.setModuleFiles(Collections.singleton(getScript("calls7.py")));
    PropagationCallGraphBuilder cgBuilder =
        (PropagationCallGraphBuilder) e.defaultCallGraphBuilder();
    CallGraph CG = cgBuilder.makeCallGraph(cgBuilder.getOptions());
    CAstCallGraphUtil.AVOID_DUMP.set(false);
    CAstCallGraphUtil.dumpCG(
        (SSAContextInterpreter) cgBuilder.getContextInterpreter(),
        cgBuilder.getPointerAnalysis(),
        CG);
  }

  protected static final Object[][] assertionsDefaultValues =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script defaultValuesTest.py"}},
        new Object[] {
          "script defaultValuesTest.py", new String[] {"script defaultValuesTest.py/defValTest"}
        },
        new Object[] {
          "script defaultValuesTest.py/defValTest",
          new String[] {"script defaultValuesTest.py/lambda1"}
        },
        new Object[] {
          "script defaultValuesTest.py/defValTest",
          new String[] {"script defaultValuesTest.py/lambda2"}
        }
      };

  @Test
  public void testDefaultValues()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<?> e =
        new PythonAnalysisEngine<Void>() {
          @Override
          public Void performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
            assert false;
            return null;
          }
        };
    e.setModuleFiles(Collections.singleton(getScript("defaultValuesTest.py")));
    PropagationCallGraphBuilder cgBuilder =
        (PropagationCallGraphBuilder) e.defaultCallGraphBuilder();
    CallGraph CG = cgBuilder.makeCallGraph(cgBuilder.getOptions());
    CAstCallGraphUtil.AVOID_DUMP.set(false);
    CAstCallGraphUtil.dumpCG(
        (SSAContextInterpreter) cgBuilder.getContextInterpreter(),
        cgBuilder.getPointerAnalysis(),
        CG);
    verifyGraphAssertions(CG, assertionsDefaultValues);
  }

  protected static final Object[][] PYTEST_ASSERTIONS =
      new Object[][] {
        new Object[] {
          ROOT, new String[] {"script test_sample.py", "script test_sample.py/test_answer"}
        },
        new Object[] {
          "script test_sample.py/test_answer", new String[] {"script test_sample.py/func"}
        },
      };

  @Test
  public void testPytestCalls()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {

    PythonAnalysisEngine<?> engine =
        new PythonAnalysisEngine<Void>() {
          @Override
          public Void performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
            assert false;
            return null;
          }
        };

    engine.setModuleFiles(singleton(getScript("test_sample.py")));

    PropagationCallGraphBuilder callGraphBuilder =
        (PropagationCallGraphBuilder) engine.defaultCallGraphBuilder();

    addPytestEntrypoints(callGraphBuilder);

    CallGraph callGraph = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

    CAstCallGraphUtil.AVOID_DUMP.set(false);
    CAstCallGraphUtil.dumpCG(
        (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
        callGraphBuilder.getPointerAnalysis(),
        callGraph);

    verifyGraphAssertions(callGraph, PYTEST_ASSERTIONS);
  }

  protected static final Object[][] PYTEST_ASSERTIONS2 =
      new Object[][] {
        new Object[] {
          ROOT,
          new String[] {
            "script test_class.py",
            "script test_class.py/TestClass",
            "$script test_class.py/TestClass/test_one:trampoline2",
            "$script test_class.py/TestClass/test_two:trampoline2"
          }
        },
      };

  @Test
  public void testPytestCalls2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<?> engine = this.makeEngine("test_class.py");
    PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();

    addPytestEntrypoints(callGraphBuilder);

    CallGraph callGraph = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

    CAstCallGraphUtil.AVOID_DUMP.set(false);
    CAstCallGraphUtil.dumpCG(
        (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
        callGraphBuilder.getPointerAnalysis(),
        callGraph);

    verifyGraphAssertions(callGraph, PYTEST_ASSERTIONS2);
  }

  protected static final Object[][] PYTEST_ASSERTIONS3 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script test_class2.py"}},
      };

  @Test
  public void testPytestCalls3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<?> engine = this.makeEngine("test_class2.py");
    PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
    addPytestEntrypoints(callGraphBuilder);
    CallGraph callGraph = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());
    CAstCallGraphUtil.AVOID_DUMP.set(false);
    CAstCallGraphUtil.dumpCG(
        (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
        callGraphBuilder.getPointerAnalysis(),
        callGraph);
    verifyGraphAssertions(callGraph, PYTEST_ASSERTIONS3);
  }

  protected static final Object[][] CLICK_ASSERTIONS =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script click_calls.py"}},
        new Object[] {
          "script click_calls.py",
          new String[] {
            "script click_calls.py/train",
          }
        }
      };

  @Test
  public void testClickCalls()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<?> engine = this.makeEngine("click_calls.py");
    PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
    CallGraph callGraph = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

    CAstCallGraphUtil.AVOID_DUMP.set(false);
    CAstCallGraphUtil.dumpCG(
        (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
        callGraphBuilder.getPointerAnalysis(),
        callGraph);
    LOGGER.info("Call graph: " + callGraph);

    verifyGraphAssertions(callGraph, CLICK_ASSERTIONS);
  }

  protected static final Object[][] ABSEIL_ASSERTIONS =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script abseil_calls.py"}},
        new Object[] {"script abseil_calls.py", new String[] {"absl/run"}},
        new Object[] {
          "absl/run",
          new String[] {
            "script abseil_calls.py/main",
          }
        }
      };

  @Test
  public void testAbseilCalls()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    PythonAnalysisEngine<?> engine = this.makeEngine("abseil_calls.py");
    PropagationCallGraphBuilder callGraphBuilder = engine.defaultCallGraphBuilder();
    CallGraph callGraph = callGraphBuilder.makeCallGraph(callGraphBuilder.getOptions());

    CAstCallGraphUtil.AVOID_DUMP.set(false);
    CAstCallGraphUtil.dumpCG(
        (SSAContextInterpreter) callGraphBuilder.getContextInterpreter(),
        callGraphBuilder.getPointerAnalysis(),
        callGraph);
    LOGGER.info("Call graph: " + callGraph);

    verifyGraphAssertions(callGraph, ABSEIL_ASSERTIONS);
  }
}
