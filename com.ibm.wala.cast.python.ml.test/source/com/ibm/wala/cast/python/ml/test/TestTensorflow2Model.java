package com.ibm.wala.cast.python.ml.test;

import static com.ibm.wala.cast.python.util.Util.addPytestEntrypoints;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
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
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.Ignore;
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
    // NOTE: Set the expected number of tensor variables to 3 once
    // https://github.com/wala/ML/issues/135 is fixed.
    test("tf2s.py", "add", 2, 2, 2, 3);
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
  public void testDecorator11()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_testing_decorator11.py", "C.returned", 1, 1, 3);
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
    test("tf2_test_dataset25.py", "f", 1, 1, 2);
    test("tf2_test_dataset25.py", "g", 1, 1, 2);
    // TODO: Change to 0, 0 once https://github.com/wala/ML/issues/165 is fixed.
    test("tf2_test_dataset25.py", "h", 1, 1, 2);
  }

  @Test
  public void testDataset26()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset26.py", "f", 1, 1, 2);
    test("tf2_test_dataset26.py", "g1", 1, 1, 2);
    test("tf2_test_dataset26.py", "g2", 1, 1, 2);
    test("tf2_test_dataset26.py", "g3", 1, 1, 2);
    // TODO: Change to 0, 0 once https://github.com/wala/ML/issues/165 is fixed.
    test("tf2_test_dataset26.py", "h", 1, 1, 2);
  }

  @Test
  public void testDataset27()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset27.py", "f", 1, 1, 2);
    test("tf2_test_dataset27.py", "g", 1, 1, 2);
    test("tf2_test_dataset27.py", "h", 1, 1, 2);
    test("tf2_test_dataset27.py", "i", 1, 1, 2);
  }

  @Test
  public void testDataset28()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_dataset28.py", "f", 1, 1, 2);
    test("tf2_test_dataset28.py", "g", 1, 1, 2);
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
    test("tf2_test_dataset31.py", "g1", 1, 1, 2);
    test("tf2_test_dataset31.py", "g2", 1, 1, 2);
    // TODO: Change to 0, 0 once https://github.com/wala/ML/issues/166 is fixed.
    test("tf2_test_dataset31.py", "h", 1, 1, 2);
    test("tf2_test_dataset31.py", "i1", 1, 1, 2);
    test("tf2_test_dataset31.py", "i2", 1, 1, 2);
    // TODO: Change to 0, 0 once https://github.com/wala/ML/issues/166 is fixed.
    test("tf2_test_dataset31.py", "j", 1, 1, 2);
    test("tf2_test_dataset31.py", "k1", 1, 1, 2);
    test("tf2_test_dataset31.py", "k2", 1, 1, 2);
    test("tf2_test_dataset31.py", "k2", 1, 1, 2);
    // TODO: Change to 0, 0 once https://github.com/wala/ML/issues/166 is fixed.
    test("tf2_test_dataset31.py", "l", 1, 1, 2);
    test("tf2_test_dataset31.py", "m1", 1, 1, 2);
    test("tf2_test_dataset31.py", "m2", 1, 1, 2);
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
    test("tensorboard_example.py", "summarize_weights", 0, 4);
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

  @Test
  public void testImport()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_import.py", "f", 1, 1, 2);
  }

  @Test
  public void testImport2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_import2.py", "f", 1, 1, 2);
  }

  @Test
  public void testImport3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_import3.py", "f", 1, 2, 2);
    test("tf2_test_import3.py", "g", 1, 1, 2);
  }

  @Test
  public void testImport4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_import4.py", "f", 1, 2, 2);
    test("tf2_test_import4.py", "g", 1, 1, 2);
  }

  @Test
  public void testImport5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_import5.py", "f", 0, 1);
    test("tf2_test_import5.py", "g", 1, 1, 2);
  }

  @Test
  public void testImport6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_import6.py", "f", 0, 1);
    test("tf2_test_import6.py", "g", 1, 1, 2);
  }

  /**
   * This is an invalid case. If there are no wildcard imports, we should resolve them like they
   * are.
   */
  @Test
  public void testImport7()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_import7.py", "f", 0, 0);
    test("tf2_test_import7.py", "g", 0, 0);
  }

  /**
   * This is an invalid case. If there are no wildcard imports, we should resolve them like they
   * are.
   */
  @Test
  public void testImport8()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_import8.py", "f", 0, 0);
    test("tf2_test_import8.py", "g", 0, 0);
  }

  @Test
  public void testImport9()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test("tf2_test_import9.py", "f", 1, 1, 2);
    test("tf2_test_import9.py", "g", 1, 1, 2);
  }

  @Test
  public void testModule()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"tf2_test_module2.py", "tf2_test_module.py"},
        "tf2_test_module2.py",
        "f",
        "",
        1,
        1,
        2);
  }

  /** This test needs a PYTHONPATH that points to `proj`. */
  @Test
  public void testModule2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj/src/__init__.py", "proj/src/tf2_test_module2a.py", "proj/src/tf2_test_module3.py"
        },
        "src/tf2_test_module2a.py",
        "f",
        "proj",
        1,
        1,
        new int[] {2});
  }

  /** This test should not need a PYTHONPATH. */
  @Test
  public void testModule3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj2/src/__init__.py", "proj2/src/tf2_test_module3a.py", "proj2/tf2_test_module4.py"
        },
        "src/tf2_test_module3a.py",
        "f",
        "proj2",
        1,
        1,
        new int[] {2});
  }

  /**
   * This test should not need a PYTHONPATH, meaning that I don't need to set one in the console
   * when I run the files.
   */
  @Test
  public void testModule4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj3/src/__init__.py",
          "proj3/src/tf2_test_module4a.py",
          "proj3/src/tf2_test_module6.py",
          "proj3/tf2_test_module5.py"
        },
        "src/tf2_test_module4a.py",
        "f",
        "proj3",
        1,
        1,
        new int[] {2});

    test(
        new String[] {
          "proj3/src/__init__.py",
          "proj3/src/tf2_test_module4a.py",
          "proj3/src/tf2_test_module6.py",
          "proj3/tf2_test_module5.py"
        },
        "src/tf2_test_module4a.py",
        "g",
        "proj3",
        1,
        1,
        new int[] {2});
  }

  @Test
  public void testModule5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"tf2_test_module4.py", "tf2_test_module3.py"},
        "tf2_test_module4.py",
        "C.f",
        "",
        1,
        1,
        3);
  }

  /** This test needs a PYTHONPATH that points to `proj4`. */
  @Test
  public void testModule6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj4/src/__init__.py", "proj4/src/tf2_test_module4a.py", "proj4/src/tf2_test_module5.py"
        },
        "src/tf2_test_module4a.py",
        "C.f",
        "proj4",
        1,
        1,
        new int[] {3});
  }

  /** This test should not need a PYTHONPATH. */
  @Test
  public void testModule7()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj5/src/__init__.py", "proj5/src/tf2_test_module5a.py", "proj5/tf2_test_module6.py"
        },
        "src/tf2_test_module5a.py",
        "C.f",
        "proj5",
        1,
        1,
        new int[] {3});
  }

  /**
   * This test should not need a PYTHONPATH, meaning that I don't need to set one in the console
   * when I run the files.
   */
  @Test
  public void testModule8()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj6/src/__init__.py",
          "proj6/src/tf2_test_module8a.py",
          "proj6/src/tf2_test_module6.py",
          "proj6/tf2_test_module7.py"
        },
        "src/tf2_test_module8a.py",
        "C.f",
        "proj6",
        1,
        1,
        new int[] {3});

    test(
        new String[] {
          "proj6/src/__init__.py",
          "proj6/src/tf2_test_module8a.py",
          "proj6/src/tf2_test_module6.py",
          "proj6/tf2_test_module7.py"
        },
        "src/tf2_test_module8a.py",
        "D.g",
        "proj6",
        1,
        1,
        new int[] {3});
  }

  @Test
  public void testModule9()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"tf2_test_module6.py", "tf2_test_module5.py"},
        "tf2_test_module6.py",
        "D.f",
        "",
        1,
        1,
        3);
  }

  @Test
  public void testModule10()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"tf2_test_module8.py", "tf2_test_module9.py", "tf2_test_module7.py"},
        "tf2_test_module9.py",
        "D.f",
        "",
        1,
        1,
        3);
  }

  /** This test needs a PYTHONPATH that points to `proj7`. */
  @Test
  public void testModule11()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj7/src/__init__.py",
          "proj7/src/tf2_test_module9a.py",
          "proj7/src/tf2_test_module9b.py",
          "proj7/src/tf2_test_module10.py"
        },
        "src/tf2_test_module9b.py",
        "D.f",
        "proj7",
        1,
        1,
        new int[] {3});
  }

  /** This test should not need a PYTHONPATH. */
  @Test
  public void testModule12()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj8/src/__init__.py",
          "proj8/src/tf2_test_module10a.py",
          "proj8/src/tf2_test_module10b.py",
          "proj8/tf2_test_module11.py"
        },
        "src/tf2_test_module10b.py",
        "D.f",
        "proj8",
        1,
        1,
        new int[] {3});
  }

  /** This test should not need a PYTHONPATH. */
  @Test
  public void testModule13()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj9/src/__init__.py",
          "proj9/src/tf2_test_module11a.py",
          "proj9/src/tf2_test_module11b.py",
          "proj9/tf2_test_module12.py"
        },
        "src/tf2_test_module11b.py",
        "D.g",
        "proj9",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test for https://github.com/wala/ML/issues/177.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule14()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj10/C/__init__.py", "proj10/C/B.py", "proj10/A.py"},
        "C/B.py",
        "f",
        "proj10",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test for https://github.com/wala/ML/issues/178.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule15()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj11/C/__init__.py", "proj11/C/B.py", "proj11/A.py"},
        "C/B.py",
        "f",
        "proj11",
        1,
        1,
        new int[] {2});
  }

  /** This test should not need a PYTHONPATH. */
  @Test
  public void testModule16()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj12/C/__init__.py", "proj12/C/B.py", "proj12/A.py"},
        "C/B.py",
        "f",
        "proj12",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test for https://github.com/wala/ML/issues/178. Multi-submodule case. See
   * https://docs.python.org/3/tutorial/modules.html#packages.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule17()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj13/C/__init__.py", "proj13/C/D/__init__.py", "proj13/C/D/B.py", "proj13/A.py"
        },
        "C/D/B.py",
        "f",
        "proj13",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test for https://github.com/wala/ML/issues/178. Multi-submodule case. See
   * https://docs.python.org/3/tutorial/modules.html#packages. This test has multiple modules in
   * different packages.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule18()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj14/C/__init__.py",
          "proj14/C/E.py",
          "proj14/C/D/__init__.py",
          "proj14/C/D/B.py",
          "proj14/A.py"
        },
        "C/D/B.py",
        "f",
        "proj14",
        1,
        1,
        new int[] {2});

    test(
        new String[] {
          "proj14/C/__init__.py",
          "proj14/C/E.py",
          "proj14/C/D/__init__.py",
          "proj14/C/D/B.py",
          "proj14/A.py"
        },
        "C/E.py",
        "g",
        "proj14",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test for https://github.com/wala/ML/issues/177.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule19()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj15/C/__init__.py", "proj15/C/D/__init__.py", "proj15/C/D/B.py", "proj15/A.py"
        },
        "C/D/B.py",
        "f",
        "proj15",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test for https://github.com/wala/ML/issues/178.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule20()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj16/C/__init__.py", "proj16/C/B.py", "proj16/A.py"},
        "C/B.py",
        "D.f",
        "proj16",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test for https://github.com/wala/ML/issues/178.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule21()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj17/C/__init__.py", "proj17/C/E/__init__.py", "proj17/C/E/B.py", "proj17/A.py"
        },
        "C/E/B.py",
        "D.f",
        "proj17",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test for https://github.com/wala/ML/issues/177.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule22()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(new String[] {"proj18/B.py", "proj18/A.py"}, "B.py", "f", "proj18", 1, 1, new int[] {2});
  }

  /**
   * Test for https://github.com/wala/ML/issues/177.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule23()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj19/C/__init__.py",
          "proj19/C/D/__init__.py",
          "proj19/C/D/E/__init__.py",
          "proj19/C/D/E/B.py",
          "proj19/A.py"
        },
        "C/D/E/B.py",
        "f",
        "proj19",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test for https://github.com/wala/ML/issues/177.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule24()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"tf2_test_module11.py", "tf2_test_module10.py"},
        "tf2_test_module11.py",
        "f",
        "",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test for https://github.com/wala/ML/issues/177.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule25()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(new String[] {"proj20/B.py", "proj20/A.py"}, "B.py", "C.f", "proj20", 1, 1, new int[] {3});
  }

  /**
   * Test for https://github.com/wala/ML/issues/177.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule26()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"tf2_test_module13.py", "tf2_test_module12.py"},
        "tf2_test_module13.py",
        "C.f",
        "",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test for https://github.com/wala/ML/issues/178.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule27()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj21/C/__init__.py",
          "proj21/C/D/__init__.py",
          "proj21/C/E.py",
          "proj21/C/D/B.py",
          "proj21/A.py"
        },
        "C/D/B.py",
        "F.f",
        "proj21",
        1,
        1,
        new int[] {3});

    test(
        new String[] {
          "proj21/C/__init__.py",
          "proj21/C/D/__init__.py",
          "proj21/C/E.py",
          "proj21/C/D/B.py",
          "proj21/A.py"
        },
        "C/E.py",
        "G.g",
        "proj21",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test for https://github.com/wala/ML/issues/177.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule28()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj22/C/__init__.py", "proj22/C/B.py", "proj22/A.py"},
        "C/B.py",
        "D.f",
        "proj22",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test for https://github.com/wala/ML/issues/178.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule29()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj23/C/__init__.py", "proj23/C/B.py", "proj23/A.py"},
        "C/B.py",
        "f",
        "proj23",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test for https://github.com/wala/ML/issues/178.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule30()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj24/C/__init__.py", "proj24/C/B.py", "proj24/A.py"},
        "C/B.py",
        "D.f",
        "proj24",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test for https://github.com/wala/ML/issues/178.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule31()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj25/C/__init__.py", "proj25/C/E/__init__.py", "proj25/C/E/B.py", "proj25/A.py"
        },
        "C/E/B.py",
        "D.f",
        "proj25",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test for https://github.com/wala/ML/issues/178.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule32()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj26/C/__init__.py", "proj26/C/B.py", "proj26/A.py"},
        "C/B.py",
        "D.f",
        "proj26",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test for https://github.com/wala/ML/issues/178.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule33()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj27/C/__init__.py", "proj27/C/D/__init__.py", "proj27/C/D/B.py", "proj27/A.py"
        },
        "C/D/B.py",
        "f",
        "proj27",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test for https://github.com/wala/ML/issues/178.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule34()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj28/C/__init__.py", "proj28/C/D/__init__.py", "proj28/C/D/B.py", "proj28/A.py"
        },
        "C/D/B.py",
        "E.f",
        "proj28",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test for https://github.com/wala/ML/issues/178.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule35()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj29/C/__init__.py", "proj29/C/B.py", "proj29/A.py"},
        "C/B.py",
        "f",
        "proj29",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test for https://github.com/wala/ML/issues/178.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule36()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj30/C/__init__.py", "proj30/C/B.py", "proj30/A.py"},
        "C/B.py",
        "f",
        "proj30",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test relative imports.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule37()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj31/C/__init__.py", "proj31/C/B.py", "proj31/C/A.py", "proj31/main.py"},
        "C/B.py",
        "f",
        "proj31",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test relative imports.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule38()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj32/C/__init__.py", "proj32/C/B.py", "proj32/C/A.py", "proj32/main.py"},
        "C/B.py",
        "f",
        "proj32",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test relative imports.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule39()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj33/C/__init__.py", "proj33/C/B.py", "proj33/C/A.py", "proj33/main.py"},
        "C/B.py",
        "D.f",
        "proj33",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test relative imports.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule40()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj34/C/__init__.py", "proj34/C/B.py", "proj34/C/A.py", "proj34/main.py"},
        "C/B.py",
        "D.f",
        "proj34",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test relative imports.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule41()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj35/E/__init__.py",
          "proj35/E/C/__init__.py",
          "proj35/E/D/__init__.py",
          "proj35/E/D/B.py",
          "proj35/E/C/A.py",
          "proj35/main.py"
        },
        "E/D/B.py",
        "f",
        "proj35",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test relative imports.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule42()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj36/E/__init__.py",
          "proj36/E/C/__init__.py",
          "proj36/E/D/__init__.py",
          "proj36/E/D/B.py",
          "proj36/E/C/A.py",
          "proj36/main.py"
        },
        "E/D/B.py",
        "F.f",
        "proj36",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test relative imports.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule43()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj37/E/__init__.py",
          "proj37/E/C/__init__.py",
          "proj37/E/D/__init__.py",
          "proj37/E/D/B.py",
          "proj37/E/C/A.py",
          "proj37/main.py"
        },
        "E/D/B.py",
        "F.f",
        "proj37",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test relative imports.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule44()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj38/E/__init__.py",
          "proj38/E/C/__init__.py",
          "proj38/E/D/__init__.py",
          "proj38/E/D/B.py",
          "proj38/E/C/A.py",
          "proj38/main.py"
        },
        "E/D/B.py",
        "f",
        "proj38",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test relative imports using wildcards.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule45()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj39/C/__init__.py", "proj39/C/B.py", "proj39/C/A.py", "proj39/main.py"},
        "C/B.py",
        "f",
        "proj39",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test relative imports using wildcards.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule46()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj40/C/__init__.py", "proj40/C/B.py", "proj40/C/A.py", "proj40/main.py"},
        "C/B.py",
        "f",
        "proj40",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test relative imports using wildcards.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule47()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj41/C/__init__.py", "proj41/C/B.py", "proj41/C/A.py", "proj41/main.py"},
        "C/B.py",
        "D.f",
        "proj41",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test relative imports using wildcards.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule48()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {"proj42/C/__init__.py", "proj42/C/B.py", "proj42/C/A.py", "proj42/main.py"},
        "C/B.py",
        "D.f",
        "proj42",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test relative imports using wildcards.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule49()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj43/E/__init__.py",
          "proj43/E/C/__init__.py",
          "proj43/E/D/__init__.py",
          "proj43/E/D/B.py",
          "proj43/E/C/A.py",
          "proj43/main.py"
        },
        "E/D/B.py",
        "f",
        "proj43",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test relative imports using wildcards.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule50()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj44/E/__init__.py",
          "proj44/E/C/__init__.py",
          "proj44/E/D/__init__.py",
          "proj44/E/D/B.py",
          "proj44/E/C/A.py",
          "proj44/main.py"
        },
        "E/D/B.py",
        "F.f",
        "proj44",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test relative imports using wildcards.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule51()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj45/E/__init__.py",
          "proj45/E/C/__init__.py",
          "proj45/E/D/__init__.py",
          "proj45/E/D/B.py",
          "proj45/E/C/A.py",
          "proj45/main.py"
        },
        "E/D/B.py",
        "F.f",
        "proj45",
        1,
        1,
        new int[] {3});
  }

  /**
   * Test relative imports using wildcards.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule52()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj46/E/__init__.py",
          "proj46/E/C/__init__.py",
          "proj46/E/D/__init__.py",
          "proj46/E/D/B.py",
          "proj46/E/C/A.py",
          "proj46/main.py"
        },
        "E/D/B.py",
        "f",
        "proj46",
        1,
        1,
        new int[] {2});
  }

  /**
   * Test relative imports.
   *
   * <p>This test should not need a PYTHONPATH.
   */
  @Test
  public void testModule53()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    test(
        new String[] {
          "proj47/E/__init__.py",
          "proj47/D/__init__.py",
          "proj47/E/C/__init__.py",
          "proj47/E/D/__init__.py",
          "proj47/E/D/B.py",
          "proj47/E/C/A.py",
          "proj47/D/B.py",
          "proj47/main.py"
        },
        "E/D/B.py",
        "f",
        "proj47",
        1,
        1,
        new int[] {2});

    test(
        new String[] {
          "proj47/E/__init__.py",
          "proj47/D/__init__.py",
          "proj47/E/C/__init__.py",
          "proj47/E/D/__init__.py",
          "proj47/E/D/B.py",
          "proj47/E/C/A.py",
          "proj47/D/B.py",
          "proj47/main.py"
        },
        "D/B.py",
        "g",
        "proj47",
        1,
        1,
        new int[] {2});
  }

  @Test
  public void testStaticMethod() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_static_method.py", "MyClass.the_static_method", 1, 1, 2);
  }

  @Test
  public void testStaticMethod2() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_static_method2.py", "MyClass.the_static_method", 1, 1, 2);
  }

  @Test
  public void testStaticMethod3() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_static_method3.py", "MyClass.the_static_method", 1, 1, 2);
  }

  @Test
  public void testStaticMethod4() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_static_method4.py", "MyClass.the_static_method", 1, 1, 2);
  }

  @Test
  public void testStaticMethod5() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_static_method5.py", "MyClass.the_static_method", 1, 1, 2);
  }

  @Test
  public void testStaticMethod6() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_static_method6.py", "MyClass.the_static_method", 1, 1, 2);
  }

  @Test
  public void testStaticMethod7() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_static_method7.py", "MyClass.the_static_method", 1, 1, 3);
  }

  @Test
  public void testStaticMethod8() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_static_method8.py", "MyClass.the_static_method", 1, 1, 3);
  }

  @Test
  public void testStaticMethod9() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_static_method9.py", "MyClass.the_static_method", 2, 2, 2, 3);
  }

  @Test
  public void testStaticMethod10() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_static_method10.py", "MyClass.the_static_method", 2, 2, 2, 3);
  }

  @Test
  public void testStaticMethod11() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_static_method11.py", "f", 1, 1, 2);
  }

  @Test
  public void testStaticMethod12() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_static_method12.py", "f", 1, 1, 2);
  }

  @Test
  public void testClassMethod() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_class_method.py", "MyClass.the_class_method", 1, 1, 3);
  }

  @Test
  public void testClassMethod2() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_class_method2.py", "MyClass.the_class_method", 1, 1, 3);
  }

  @Test
  public void testClassMethod3() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_class_method3.py", "MyClass.f", 1, 1, 2);
  }

  @Test
  public void testClassMethod4() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_class_method4.py", "MyClass.f", 1, 1, 2);
  }

  @Test
  public void testClassMethod5() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_class_method5.py", "MyClass.f", 1, 1, 2);
  }

  @Test
  public void testAbstractMethod() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_abstract_method.py", "D.f", 1, 1, 3);
    test("tf2_test_abstract_method.py", "C.f", 1, 1, 3);
  }

  @Test
  public void testAbstractMethod2() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_abstract_method2.py", "D.f", 1, 1, 3);
    test("tf2_test_abstract_method2.py", "C.f", 1, 1, 3);
  }

  @Test
  public void testAbstractMethod3() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_abstract_method3.py", "C.f", 1, 1, 3);
  }

  @Test
  public void testDecoratedMethod() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_decorated_method.py", "f", 1, 1, 2);
  }

  @Ignore("Enable once once https://github.com/wala/ML/issues/188 is fixed.")
  @Test
  public void testDecoratedMethod2() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_decorated_method2.py", "f", 1, 1, 2);
  }

  @Ignore("Enable once once https://github.com/wala/ML/issues/190 is fixed.")
  @Test
  public void testDecoratedMethod3() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_decorated_method3.py", "raffi", 1, 1, 2);
  }

  @Test
  public void testDecoratedMethod4() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_decorated_method4.py", "raffi", 1, 1, 2);
  }

  @Test
  public void testDecoratedMethod5() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_decorated_method5.py", "raffi", 1, 1, 2);
  }

  @Test
  public void testDecoratedMethod6() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_decorated_method6.py", "f", 1, 1, 2);
  }

  @Test
  public void testDecoratedMethod7() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_decorated_method7.py", "f", 1, 1, 2);
  }

  @Test
  public void testDecoratedMethod8() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_decorated_method8.py", "f", 1, 1, 2);
  }

  /** This decorator isn't defined. Thus, we shouldn't have a CG node for it. */
  @Ignore(
      "We now require nodes for functions under test. Otherwise, a test could pass even though the"
          + " function doesn't exist.")
  @Test
  public void testDecoratedMethod9() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_decorated_method9.py", "f", 0, 0);
  }

  @Ignore("Enable once once https://github.com/wala/ML/issues/190 is fixed.")
  @Test
  public void testDecoratedMethod10() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_decorated_method10.py", "f", 1, 1, 2);
  }

  @Test
  public void testDecoratedMethod11() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_decorated_method11.py", "f", 1, 1, 2);
  }

  @Test
  public void testDecoratedMethod12() throws ClassHierarchyException, CancelException, IOException {
    // NOTE: Change to 1, 1, 2 once https://github.com/wala/ML/issues/188 is fixed.
    test("tf2_test_decorated_method12.py", "f", 0, 0);
  }

  @Ignore("Enable once once https://github.com/wala/ML/issues/190 is fixed.")
  @Test
  public void testDecoratedMethod13() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_decorated_method13.py", "f", 1, 1, 2);
  }

  @Test
  public void testDecoratedFunctions()
      throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_decorated_functions.py", "dummy_fun", 1, 1, 2);
    test("tf2_test_decorated_functions.py", "dummy_test", 1, 1, 2);
    test("tf2_test_decorated_functions.py", "test_function", 1, 1, 2);
    test("tf2_test_decorated_functions.py", "test_function2", 1, 1, 2);
    test("tf2_test_decorated_functions.py", "test_function3", 1, 1, 2);
    test("tf2_test_decorated_functions.py", "test_function4", 1, 1, 2);
  }

  /** Test a pytest with decorators. */
  @Test
  public void testDecoratedFunctions2()
      throws ClassHierarchyException, CancelException, IOException {
    test("test_decorated_functions.py", "test_dummy", 0, 0);
  }

  @Test
  public void testReshape() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_reshape.py", "f", 1, 1, 2);
  }

  @Test
  public void testReshape2() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_reshape2.py", "f", 1, 1, 2);
  }

  @Test
  public void testReshape3() throws ClassHierarchyException, CancelException, IOException {
    test("tf2_test_reshape3.py", "f", 1, 1, 2);
  }

  private void test(
      String filename,
      String functionName,
      int expectedNumberOfTensorParameters,
      int expectedNumberOfTensorVariables,
      int... expectedTensorParameterValueNumbers)
      throws ClassHierarchyException, CancelException, IOException {
    test(
        new String[] {filename},
        filename,
        functionName,
        "",
        expectedNumberOfTensorParameters,
        expectedNumberOfTensorVariables,
        expectedTensorParameterValueNumbers);
  }

  private void test(
      String[] projectFilenames,
      String filename,
      String functionName,
      String pythonPath,
      int expectedNumberOfTensorParameters,
      int expectedNumberOfTensorVariables,
      int... expectedTensorParameterValueNumbers)
      throws ClassHierarchyException, CancelException, IOException {
    List<File> pathFiles = this.getPathFiles(pythonPath);
    PythonAnalysisEngine<TensorTypeAnalysis> E = makeEngine(pathFiles, projectFilenames);
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

    final String functionSignature =
        "script " + filename.replace('/', '.') + "." + functionName + ".do()LRoot;";

    // check that the function exists in the call graph.
    assertTrue(
        "Function must exist in call graph.",
        CG.stream()
            .map(CGNode::getMethod)
            .map(IMethod::getSignature)
            .anyMatch(s -> s.equals(functionSignature)));

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

  /**
   * Extracts a {@link List} of {@link File}s from the given {@link String} representing a list of
   * paths. Each path is separated by a colon.
   *
   * @param string A colon-separated list of paths.
   * @return {@link List} of {@link File}s constructed by parsing the given {@link String}.
   */
  private List<File> getPathFiles(String string) {
    if (string == null || string.isEmpty() || string.isBlank()) return emptyList();

    return Arrays.asList(string.split(":")).stream()
        .map(
            s -> {
              File f = new File(s);

              if (f.exists()) return f;

              try {
                URL url = new URL(s);
                return new File(new FileProvider().filePathFromURL(url));
              } catch (MalformedURLException e) {
                try {
                  URL resource = this.getClass().getResource("/" + string);
                  String path = resource.getPath();
                  return new File(path);
                } catch (Exception e1) {
                  throw new RuntimeException(e1);
                }
              }
            })
        .collect(toList());
  }
}
