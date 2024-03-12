package com.ibm.wala.cast.python.ml.test;

import static com.ibm.wala.cast.python.util.Util.addPytestEntrypoints;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ipa.callgraph.PythonSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.python.ml.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.ml.analysis.TensorVariable;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.Test;

/** Test TF2 APIs. */
public class TestTensorflow2Model extends TestPythonMLCallGraphShape {

  private static final Logger LOGGER = Logger.getLogger(TestTensorflow2Model.class.getName());

  @Test
  public void test()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testB()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2b.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testC()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2c.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testD()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2d.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testD2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2d2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testD3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2d3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testD4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2d4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testD5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2d5.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testE()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2e.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testE2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2e2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testE3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2e3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testE4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2e4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testE5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2e5.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testE6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2e6.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testE7()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2e7.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testE8()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2e8.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testF()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2f.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testF2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2f2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testF3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2f3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testG()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2g.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testG2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2g2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testH()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2h.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testH2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2h2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testI()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2i.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testI2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2i2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testJ()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2j.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testJ2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2j2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testK()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2k.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testK2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2k2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testL()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2l.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testL2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2l2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testM()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2m.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testM2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2m2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testN()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2n.py", "func2", 1, 1, 2);
  }

  @Test
  public void testN2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2n2.py", "func2", 1, 1, 2);
  }

  @Test
  public void testN3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2n3.py", "func2", 1, 1, 2);
  }

  @Test
  public void testO()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2o.py", "add", 2, 3, 2, 3);
  }

  @Test
  public void testO2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2o2.py", "add", 2, 3, 2, 3);
  }

  @Test
  public void testP()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2p.py", "value_index", 2, 2, 2, 3);
  }

  @Test
  public void testP2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2p2.py", "value_index", 2, 2, 2, 3);
  }

  @Test
  public void testQ()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2q.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testR()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2r.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testS()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        "tf2s.py", "add", 0,
        0); // NOTE: Set the expected number of tensor parameters, variables, and tensor parameter
    // value numbers to 2, 3, and 2 and 3, respectively, when
    // https://github.com/wala/ML/issues/65 is fixed.
  }

  @Test
  public void testT()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2t.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testU()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2u.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testU2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2u2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testU3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2u3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testV()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2v.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testV2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2v2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testV3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2v3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testV4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2v4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testV5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2v5.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testW()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2w.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testW2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2w2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testW3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2w3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testW4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2w4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testX()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2x.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testX2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2x2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testX3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2x3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testY()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2y.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testY2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2y2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testY3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2y3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testY4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2y4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testY5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2y5.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testY6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2y6.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testZ()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2z.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testZ2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2z2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testZ3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2z3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testZ4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2z4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testAA()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2aa.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testAA2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2aa2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testAA3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2aa3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testAA4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2aa4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testAA5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2aa5.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testAA6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2aa6.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testAA7()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2aa7.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testAA8()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2aa8.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testBB()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2bb.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testBB2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2bb2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testBB3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2bb3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testBB4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2bb4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testCC()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2cc.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testCC2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2cc2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testCC3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2cc3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testDD()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2dd.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testDD2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2dd2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testEE()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2ee.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testEE2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2ee2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testFF()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2ff.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testFF2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2ff2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testGG()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2gg.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testGG2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2gg2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testGG3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2gg3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testHH()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2hh.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testHH2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2hh2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testHH3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2hh3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testHH4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2hh4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testII()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2ii.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testII2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2ii2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testII3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2ii3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testJJ()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2jj.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testJJ2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2jj2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testKK()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2kk.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testKK2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2kk2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testLL()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2ll.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testLL2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2ll2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testLL3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2ll3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testMM()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2mm.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testMM2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2mm2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testNN()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2nn.py", "value_index", 2, 2, 2, 3);
  }

  @Test
  public void testNN2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2nn2.py", "value_index", 2, 2, 2, 3);
  }

  @Test
  public void testNN3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2nn3.py", "value_index", 2, 2, 2, 3);
  }

  @Test
  public void testNN4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2nn4.py", "value_index", 2, 2, 2, 3);
  }

  @Test
  public void testOO()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2oo.py", "func2", 1, 1, 2);
  }

  @Test
  public void testOO2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2oo2.py", "func2", 1, 1, 2);
  }

  @Test
  public void testOO3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2oo3.py", "func2", 1, 1, 2);
  }

  @Test
  public void testOO4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2oo4.py", "func2", 1, 1, 2);
  }

  @Test
  public void testDecorator()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_testing_decorator.py", "returned", 1, 1, 2);
  }

  @Test
  public void testDecorator2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_testing_decorator2.py", "returned", 1, 1, 2);
  }

  @Test
  public void testDecorator3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_testing_decorator3.py", "returned", 1, 1, 2);
  }

  @Test
  public void testDecorator4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_testing_decorator4.py", "returned", 1, 1, 2);
  }

  @Test
  public void testDecorator5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_testing_decorator5.py", "returned", 1, 1, 2);
  }

  @Test
  public void testDecorator6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_testing_decorator6.py", "returned", 1, 1, 2);
  }

  @Test
  public void testDecorator7()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_testing_decorator7.py", "returned", 1, 1, 2);
  }

  @Test
  public void testDecorator8()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_testing_decorator8.py", "returned", 1, 1, 2);
  }

  @Test
  public void testDecorator9()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_testing_decorator9.py", "returned", 1, 1, 2);
  }

  @Test
  public void testDecorator10()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_testing_decorator10.py", "returned", 1, 1, 2);
  }

  @Test
  public void testDataset()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    // FIXME: Test tf2_test_dataset.py really has three tensors in its dataset. We are currently
    // treating it as one. But, in the literal case, it should be possible to model it like the list
    // tests below.
    test("tf2_test_dataset.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testDataset2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset2.py", "add", 2, 2, 2, 3);
  }

  /** This is not a legal case. */
  @Test
  public void testDataset3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset3.py", "add", 2, 2, 2, 3);
  }

  /** This is not a legal case. */
  @Test
  public void testDataset4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testDataset5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset5.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testDataset6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset6.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testDataset7()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset7.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testDataset8()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset8.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testDataset9()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset9.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testDataset10()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset10.py", "add", 2, 2, 2, 3);
  }

  /**
   * Test enumerating a dataset (https://github.com/wala/ML/issues/140). The first element of the
   * tuple returned isn't a tensor.
   */
  @Test
  public void testDataset11()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset11.py", "f", 0, 0);
    test("tf2_test_dataset11.py", "g", 1, 1, 2);
  }

  /**
   * Test enumerating a dataset (https://github.com/wala/ML/issues/140). The first element of the
   * tuple returned isn't a tensor.
   */
  @Test
  public void testDataset12()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset12.py", "f", 0, 0);
    test("tf2_test_dataset12.py", "g", 1, 1, 2);
  }

  /**
   * Test enumerating a dataset (https://github.com/wala/ML/issues/140). The first element of the
   * tuple returned isn't a tensor.
   */
  @Test
  public void testDataset13()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset13.py", "f", 0, 0);
    test("tf2_test_dataset13.py", "g", 1, 1, 2);
  }

  /**
   * Test enumerating a dataset (https://github.com/wala/ML/issues/140). The first element of the
   * tuple returned isn't a tensor.
   */
  @Test
  public void testDataset14()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset14.py", "f", 0, 0);
    test("tf2_test_dataset14.py", "g", 1, 1, 2);
  }

  /**
   * Test enumerating a dataset (https://github.com/wala/ML/issues/140). The first element of the
   * tuple returned isn't a tensor.
   */
  @Test
  public void testDataset15()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset14.py", "f", 0, 0);
    test("tf2_test_dataset14.py", "g", 1, 1, 2);
  }

  /** Test a dataset that uses an iterator. */
  @Test
  public void testDataset16()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset16.py", "add", 2, 2, 2, 3);
  }

  /** Test a dataset that uses an iterator. */
  @Test
  public void testDataset17()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset17.py", "add", 2, 2, 2, 3);
    test("tf2_test_dataset17.py", "f", 1, 1, 2);
  }

  /** Test a dataset that uses an iterator. */
  @Test
  public void testDataset18()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset18.py", "add", 2, 2, 2, 3);
    test("tf2_test_dataset18.py", "f", 1, 1, 2);
    test("tf2_test_dataset18.py", "g", 0, 2);
  }

  /** Test a dataset that uses an iterator. */
  @Test
  public void testDataset19()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset19.py", "distributed_train_step", 1, 1, 2);
  }

  @Test
  public void testDataset20()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset20.py", "f", 1, 1, 2);
  }

  @Test
  public void testDataset21()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset21.py", "f", 1, 1, 2);
  }

  @Test
  public void testDataset22()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset22.py", "f", 1, 1, 2);
  }

  @Test
  public void testDataset23()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset23.py", "f", 1, 1, 2);
    test("tf2_test_dataset23.py", "g", 1, 1, 2);
  }

  @Test
  public void testDataset24()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset24.py", "f", 1, 1, 2);
    test("tf2_test_dataset24.py", "g", 1, 1, 2);
  }

  @Test
  public void testDataset25()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    // TODO: Change to 1, 1, 2 once https://github.com/wala/ML/issues/165 is fixed.
    test("tf2_test_dataset25.py", "f", 0, 0);
    // TODO: Change to 1, 1, 2 once https://github.com/wala/ML/issues/165 is fixed.
    test("tf2_test_dataset25.py", "g", 0, 0);
    // TODO: Change to 0, 0 once https://github.com/wala/ML/issues/165 is fixed.
    test("tf2_test_dataset25.py", "h", 1, 1, 2);
  }

  @Test
  public void testDataset26()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    // TODO: Change to 1, 1, 2 once https://github.com/wala/ML/issues/165 is fixed.
    test("tf2_test_dataset26.py", "f", 0, 0);
    // TODO: Change to 1, 1, 2 once https://github.com/wala/ML/issues/165 is fixed.
    test("tf2_test_dataset26.py", "g1", 0, 0);
    // TODO: Change to 1, 1, 2 once https://github.com/wala/ML/issues/165 is fixed.
    test("tf2_test_dataset26.py", "g2", 0, 0);
    // TODO: Change to 1, 1, 2 once https://github.com/wala/ML/issues/165 is fixed.
    test("tf2_test_dataset26.py", "g3", 0, 0);
    // TODO: Change to 0, 0 once https://github.com/wala/ML/issues/165 is fixed.
    test("tf2_test_dataset26.py", "h", 1, 1, 2);
  }

  @Test
  public void testDataset27()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset27.py", "f", 1, 1, 2);
    // TODO: Change to 1, 1, 2 when https://github.com/wala/ML/issues/164 is fixed:
    test("tf2_test_dataset27.py", "g", 0, 0);
    // TODO: Change to 1, 1, 2 when https://github.com/wala/ML/issues/164 is fixed:
    test("tf2_test_dataset27.py", "h", 0, 0);
    // TODO: Change to 1, 1, 2 when https://github.com/wala/ML/issues/164 is fixed:
    test("tf2_test_dataset27.py", "i", 0, 0);
  }

  @Test
  public void testDataset28()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    // TODO: Change to 1, 1, 2 when https://github.com/wala/ML/issues/164 is fixed:
    test("tf2_test_dataset28.py", "f", 0, 0);
    // TODO: Change to 1, 1, 2 when https://github.com/wala/ML/issues/164 is fixed:
    test("tf2_test_dataset28.py", "g", 0, 0);
    // TODO: Change to 0, 0 when https://github.com/wala/ML/issues/164 is fixed:
    test("tf2_test_dataset28.py", "h", 1, 1, 2);
  }

  @Test
  public void testDataset29()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset29.py", "f", 1, 1, 2);
  }

  @Test
  public void testDataset30()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset30.py", "f", 1, 1, 2);
  }

  @Test
  public void testDataset31()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    // TODO: Change to 0, 0 once https://github.com/wala/ML/issues/166 is fixed.
    test("tf2_test_dataset31.py", "f", 1, 1, 2);
    // TODO: Change to 1, 1, 2 once https://github.com/wala/ML/issues/166 is fixed.
    test("tf2_test_dataset31.py", "g1", 0, 0);
    // TODO: Change to 1, 1, 2 once https://github.com/wala/ML/issues/166 is fixed.
    test("tf2_test_dataset31.py", "g2", 0, 0);
    // TODO: Change to 0, 0 once https://github.com/wala/ML/issues/166 is fixed.
    test("tf2_test_dataset31.py", "h", 1, 1, 2);
    // TODO: Change to 1, 1, 2 once https://github.com/wala/ML/issues/166 is fixed.
    test("tf2_test_dataset31.py", "i1", 0, 0);
    // TODO: Change to 1, 1, 2 once https://github.com/wala/ML/issues/166 is fixed.
    test("tf2_test_dataset31.py", "i2", 0, 0);
    // TODO: Change to 0, 0 once https://github.com/wala/ML/issues/166 is fixed.
    test("tf2_test_dataset31.py", "j", 1, 1, 2);
    // TODO: Change to 1, 1, 2 once https://github.com/wala/ML/issues/166 is fixed.
    test("tf2_test_dataset31.py", "k1", 0, 0);
    // TODO: Change to 1, 1, 2 once https://github.com/wala/ML/issues/166 is fixed.
    test("tf2_test_dataset31.py", "k2", 0, 0);
    // TODO: Change to 1, 1, 2 once https://github.com/wala/ML/issues/166 is fixed.
    test("tf2_test_dataset31.py", "k2", 0, 0);
    // TODO: Change to 0, 0 once https://github.com/wala/ML/issues/166 is fixed.
    test("tf2_test_dataset31.py", "l", 1, 1, 2);
    // TODO: Change to 1, 1, 2 once https://github.com/wala/ML/issues/166 is fixed.
    test("tf2_test_dataset31.py", "m1", 0, 0);
    // TODO: Change to 1, 1, 2 once https://github.com/wala/ML/issues/166 is fixed.
    test("tf2_test_dataset31.py", "m2", 0, 0);
  }

  @Test
  public void testDataset32()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset32.py", "f", 1, 1, 2);
  }

  @Test
  public void testDataset33()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset33.py", "f", 1, 1, 2);
  }

  /**
   * Test enumerating a dataset (https://github.com/wala/ML/issues/140). The first element of the
   * tuple returned isn't a tensor.
   */
  @Test
  public void testTensorboardExample()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tensorboard_example.py", "summarize_weights", 0, 12);
  }

  @Test
  public void testTensorList()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_tensor_list.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTensorList2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_tensor_list2.py", "add", 0, 0);
  }

  @Test
  public void testTensorList3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        "tf2_test_tensor_list3.py",
        "add",
        0,
        0); // NOTE: Change to 2, 2, 2, 3 once https://github.com/wala/ML/issues/136 is fixed.
  }

  @Test
  public void testTensorList4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_tensor_list4.py", "add", 0, 0);
  }

  @Test
  public void testTensorList5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_tensor_list5.py", "add", 0, 0);
  }

  @Test
  public void testModelCall()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_model_call.py", "SequentialModel.__call__", 1, 1, 3);
  }

  @Test
  public void testModelCall2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_model_call2.py", "SequentialModel.call", 1, 1, 3);
  }

  @Test
  public void testModelCall3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_model_call3.py", "SequentialModel.call", 1, 1, 3);
  }

  @Test
  public void testModelCall4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_model_call4.py", "SequentialModel.__call__", 1, 1, 3);
  }

  @Test
  public void testModelAttributes()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_model_attributes.py", "f", 1, 1, 2);
  }

  @Test
  public void testModelAttributes2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_model_attributes2.py", "f", 1, 1, 2);
  }

  @Test
  public void testModelAttributes3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_model_attributes3.py", "f", 1, 1, 2);
  }

  @Test
  public void testModelAttributes4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_model_attributes4.py", "f", 1, 1, 2);
  }

  @Test
  public void testModelAttributes5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_model_attributes5.py", "f", 1, 1, 2);
  }

  @Test
  public void testModelAttributes6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_model_attributes6.py", "f", 1, 1, 2);
  }

  @Test
  public void testCallbacks()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_callbacks.py", "replica_fn", 1, 1, 2);
  }

  @Test
  public void testCallbacks2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_callbacks2.py", "replica_fn", 1, 1, 2);
  }

  @Test
  public void testGanTutorial()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tensorflow_gan_tutorial.py", "train_step", 1, 2, 2);
  }

  @Test
  public void testGanTutorial2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tensorflow_gan_tutorial2.py", "train_step", 1, 2, 2);
  }

  @Test
  public void testEagerExecution()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tensorflow_eager_execution.py", "MyModel.call", 1, 1, 3);
  }

  @Test
  public void testNeuralNetwork()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("neural_network.py", "NeuralNet.call", 1, 1, 3);
  }

  @Test
  public void testNeuralNetwork2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        "neural_network.py",
        "cross_entropy_loss",
        1,
        8,
        3); // NOTE: Change to 2 tensor parameters once https://github.com/wala/ML/issues/127 is
    // fixed. Values 2 and 3 will correspond to the tensor parameters.
  }

  @Test
  public void testNeuralNetwork3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("neural_network.py", "run_optimization", 2, 3, 2, 3);
  }

  @Test
  public void testNeuralNetwork4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        "neural_network.py",
        "accuracy",
        1,
        3,
        3); // NOTE: Change to 2 tensor parameters and 5 tensor variables once
    // https://github.com/wala/ML/issues/127 is fixed. Values 2 and 3 will correspond to the
    // tensor parameters.
  }

  @Test
  public void testAutoencoder()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("autoencoder.py", "encoder", 1, 18, 2);
  }

  @Test
  public void testAutoencoder2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("autoencoder.py", "mean_square", 2, 2, 2, 3);
  }

  @Test
  public void testAutoencoder3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("autoencoder.py", "run_optimization", 1, 3, 2);
  }

  @Test
  public void testAutoencoder4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("autoencoder.py", "decoder", 1, 18, 2);
  }

  @Test
  public void testSigmoid()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_sigmoid.py", "f", 1, 1, 2);
  }

  @Test
  public void testSigmoid2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_sigmoid2.py", "f", 1, 1, 2);
  }

  @Test
  public void testAdd()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_add.py", "f", 1, 1, 2);
  }

  @Test
  public void testAdd2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_add2.py", "f", 1, 1, 2);
  }

  @Test
  public void testAdd3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_add3.py", "f", 1, 1, 2);
  }

  @Test
  public void testAdd4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_add4.py", "f", 1, 1, 2);
  }

  @Test
  public void testAdd5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_add5.py", "f", 1, 1, 2);
  }

  @Test
  public void testAdd6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_add6.py", "f", 1, 1, 2);
  }

  @Test
  public void testMultiGPUTraining()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("multigpu_training.py", "run_optimization", 2, 4, 2, 3);
  }

  @Test
  public void testMultiGPUTraining2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        "multigpu_training.py",
        "average_gradients",
        0,
        0); // NOTE: Change to 1, 1, 2 once https://github.com/wala/ML/issues/136 is fixed.
  }

  @Test
  public void testReduceMean()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_reduce_mean.py", "f", 1, 1, 2);
  }

  @Test
  public void testReduceMean2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_reduce_mean.py", "g", 1, 1, 2);
  }

  @Test
  public void testReduceMean3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_reduce_mean.py", "h", 1, 1, 2);
  }

  @Test
  public void testGradient()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_gradient.py", "f", 1, 1, 2);
  }

  @Test
  public void testGradient2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_gradient2.py", "f", 1, 1, 2);
  }

  @Test
  public void testMultiply()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_multiply.py", "f", 1, 1, 2);
  }

  @Test
  public void testMultiply2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_multiply2.py", "f", 1, 1, 2);
  }

  @Test
  public void testSparseSoftmaxCrossEntropyWithLogits()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_sparse_softmax_cross_entropy_with_logits.py", "f", 1, 1, 2);
  }

  @Test
  public void testRelu()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_relu.py", "f", 1, 1, 2);
  }

  @Test
  public void testTFRange()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_tf_range.py", "f", 1, 1, 2);
  }

  @Test
  public void testTFRange2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_tf_range2.py", "f", 1, 1, 2);
  }

  @Test
  public void testTFRange3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("test_tf_range.py", "f", 1, 1, 2);
  }

  private void test(
      String filename,
      String functionName,
      int expectedNumberOfTensorParameters,
      int expectedNumberOfTensorVariables,
      int... expectedTensorParameterValueNumbers)
      throws ClassHierarchyException, CancelException, IOException {
    PythonAnalysisEngine<TensorTypeAnalysis> E = makeEngine(filename);
    PythonSSAPropagationCallGraphBuilder builder = E.defaultCallGraphBuilder();

    addPytestEntrypoints(builder);

    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    assertNotNull(CG);

    if (LOGGER.isLoggable(Level.FINE)) {
      CAstCallGraphUtil.AVOID_DUMP = false;
      CAstCallGraphUtil.dumpCG(
          ((SSAPropagationCallGraphBuilder) builder).getCFAContextInterpreter(),
          builder.getPointerAnalysis(),
          CG);
      LOGGER.fine("Call graph:\n" + CG);
    }

    TensorTypeAnalysis analysis = E.performAnalysis(builder);

    LOGGER.info("Tensor analysis: " + analysis);

    // Create a mapping from function signatures to pointer keys.
    Map<String, Set<LocalPointerKey>> functionSignatureToPointerKeys = new HashMap<>();

    // Create a mapping from function signatures to tensor variables.
    Map<String, Set<TensorVariable>> functionSignatureToTensorVariables = new HashMap<>();

    // for each pointer key, tensor variable pair.
    analysis.forEach(
        p -> {
          PointerKey pointerKey = p.fst;

          if (pointerKey instanceof LocalPointerKey) {
            LocalPointerKey localPointerKey = (LocalPointerKey) pointerKey;

            // get the call graph node associated with the pointer key.
            CGNode node = localPointerKey.getNode();

            // get the method associated with the call graph node.
            IMethod method = node.getMethod();
            String methodSignature = method.getSignature();

            // associate the method to the pointer key.
            functionSignatureToPointerKeys.compute(
                methodSignature,
                (k, v) -> {
                  if (v == null) v = new HashSet<>();
                  v.add(localPointerKey);
                  return v;
                });

            TensorVariable tensorVariable = p.snd;

            // associate the method to the tensor variables.
            functionSignatureToTensorVariables.compute(
                methodSignature,
                (k, v) -> {
                  if (v == null) v = new HashSet<>();
                  v.add(tensorVariable);
                  return v;
                });
          } else LOGGER.warning(() -> "Encountered: " + pointerKey.getClass());
        });

    final String functionSignature = "script " + filename + "." + functionName + ".do()LRoot;";

    // get the tensor variables for the function.
    Set<TensorVariable> functionTensorVariables =
        functionSignatureToTensorVariables.getOrDefault(functionSignature, emptySet());

    assertEquals(expectedNumberOfTensorVariables, functionTensorVariables.size());

    // check value numbers.
    assertEquals(
        "Each tensor parameter should have a unique value number.",
        expectedNumberOfTensorParameters,
        expectedTensorParameterValueNumbers.length);

    // get the pointer keys for the function by their contexts.
    Map<Context, Set<LocalPointerKey>> contextToFunctionParameterPointerKeys =
        functionSignatureToPointerKeys.getOrDefault(functionSignature, emptySet()).stream()
            .filter(LocalPointerKey::isParameter)
            .collect(groupingBy(lpk -> lpk.getNode().getContext(), toSet()));

    assertTrue(
        "Because tensor parameters are inferred via function arguments, we need at least one"
            + " calling context if we are expecting at least one tensor parameter.",
        expectedNumberOfTensorParameters <= 0 || contextToFunctionParameterPointerKeys.size() > 0);

    for (Context ctx : contextToFunctionParameterPointerKeys.keySet()) {
      // check tensor parameters.
      Set<LocalPointerKey> functionParameterPointerKeys =
          contextToFunctionParameterPointerKeys.get(ctx);

      assertEquals(expectedNumberOfTensorParameters, functionParameterPointerKeys.size());

      // check value numbers.
      Set<Integer> actualParameterValueNumberSet =
          functionParameterPointerKeys.stream()
              .map(LocalPointerKey::getValueNumber)
              .collect(Collectors.toSet());

      assertEquals(
          expectedTensorParameterValueNumbers.length, actualParameterValueNumberSet.size());

      Arrays.stream(expectedTensorParameterValueNumbers)
          .forEach(
              ev ->
                  assertTrue(
                      "Expecting " + actualParameterValueNumberSet + " to contain " + ev + ".",
                      actualParameterValueNumberSet.contains(ev)));
    }
  }
}
