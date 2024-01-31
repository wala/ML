package com.ibm.wala.cast.python.ml.test;

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

  private static final Logger logger = Logger.getLogger(TestTensorflow2Model.class.getName());

  @Test
  public void testTf2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2b()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2b.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2c()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2c.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2d()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2d.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2d2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2d2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2d3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2d3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2d4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2d4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2d5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2d5.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2e()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2e.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2e2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2e2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2e3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2e3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2e4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2e4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2e5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2e5.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2e6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2e6.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2e7()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2e7.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2e8()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2e8.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2f()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2f.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2f2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2f2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2f3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2f3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2g()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2g.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2g2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2g2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2h()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2h.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2h2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2h2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2i()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2i.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2i2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2i2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2j()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2j.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2j2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2j2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2k()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2k.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2k2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2k2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2l()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2l.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2l2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2l2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2m()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2m.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2m2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2m2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2n()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2n.py", "func2", 1, 1, 2);
  }

  @Test
  public void testTf2n2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2n2.py", "func2", 1, 1, 2);
  }

  @Test
  public void testTf2n3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2n3.py", "func2", 1, 1, 2);
  }

  @Test
  public void testTf2o()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2o.py", "add", 2, 3, 2, 3);
  }

  @Test
  public void testTf2o2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2o2.py", "add", 2, 3, 2, 3);
  }

  @Test
  public void testTf2p()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2p.py", "value_index", 2, 2, 2, 3);
  }

  @Test
  public void testTf2p2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2p2.py", "value_index", 2, 2, 2, 3);
  }

  @Test
  public void testTf2q()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2q.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2r()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2r.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2s()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2(
        "tf2s.py", "add", 0,
        0); // NOTE: Set the expected number of tensor parameters, variables, and tensor parameter
    // value numbers to 2, 3, and 2 and 3, respectively, when
    // https://github.com/wala/ML/issues/65 is fixed.
  }

  @Test
  public void testTf2t()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2t.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2u()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2u.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2u2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2u2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2u3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2u3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2v()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2v.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2v2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2v2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2v3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2v3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2v4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2v4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2v5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2v5.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2w()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2w.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2w2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2w2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2w3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2w3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2w4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2w4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2x()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2x.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2x2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2x2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2x3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2x3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2y()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2y.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2y2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2y2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2y3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2y3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2y4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2y4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2y5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2y5.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2y6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2y6.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2z()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2z.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2z2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2z2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2z3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2z3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2z4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2z4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2aa()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2aa.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2aa2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2aa2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2aa3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2aa3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2aa4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2aa4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2aa5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2aa5.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2aa6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2aa6.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2aa7()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2aa7.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2aa8()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2aa8.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2bb()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2bb.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2bb2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2bb2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2bb3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2bb3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2bb4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2bb4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2cc()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2cc.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2cc2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2cc2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2cc3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2cc3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2dd()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2dd.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2dd2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2dd2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2ee()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2ee.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2ee2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2ee2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2ff()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2ff.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2ff2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2ff2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2gg()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2gg.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2gg2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2gg2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2gg3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2gg3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2hh()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2hh.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2hh2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2hh2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2hh3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2hh3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2hh4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2hh4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2ii()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2ii.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2ii2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2ii2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2ii3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2ii3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2jj()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2jj.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2jj2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2jj2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2kk()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2kk.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2kk2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2kk2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2ll()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2ll.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2ll2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2ll2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2ll3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2ll3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2mm()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2mm.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2mm2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2mm2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2nn()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2nn.py", "value_index", 2, 2, 2, 3);
  }

  @Test
  public void testTf2nn2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2nn2.py", "value_index", 2, 2, 2, 3);
  }

  @Test
  public void testTf2nn3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2nn3.py", "value_index", 2, 2, 2, 3);
  }

  @Test
  public void testTf2nn4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2nn4.py", "value_index", 2, 2, 2, 3);
  }

  @Test
  public void testTf2oo()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2oo.py", "func2", 1, 1, 2);
  }

  @Test
  public void testTf2oo2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2oo2.py", "func2", 1, 1, 2);
  }

  @Test
  public void testTf2oo3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2oo3.py", "func2", 1, 1, 2);
  }

  @Test
  public void testTf2oo4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2oo4.py", "func2", 1, 1, 2);
  }

  @Test
  public void testTf2TestingDecorator()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_testing_decorator.py", "returned", 1, 1, 2);
  }

  @Test
  public void testTf2TestingDecorator2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_testing_decorator2.py", "returned", 1, 1, 2);
  }

  @Test
  public void testTf2TestingDecorator3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_testing_decorator3.py", "returned", 1, 1, 2);
  }

  @Test
  public void testTf2TestingDecorator4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_testing_decorator4.py", "returned", 1, 1, 2);
  }

  @Test
  public void testTf2TestingDecorator5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_testing_decorator5.py", "returned", 1, 1, 2);
  }

  @Test
  public void testTf2TestingDecorator6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_testing_decorator6.py", "returned", 1, 1, 2);
  }

  @Test
  public void testTf2TestingDecorator7()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_testing_decorator7.py", "returned", 1, 1, 2);
  }

  @Test
  public void testTf2TestingDecorator8()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_testing_decorator8.py", "returned", 1, 1, 2);
  }

  @Test
  public void testTf2TestingDecorator9()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_testing_decorator9.py", "returned", 1, 1, 2);
  }

  @Test
  public void testTf2TestingDecorator10()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_testing_decorator10.py", "returned", 1, 1, 2);
  }

  @Test
  public void testTf2TestDataset()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    // FIXME: Test tf2_test_dataset.py really has three tensors in its dataset. We are currently
    // treating it as one. But, in the literal case, it should be possible to model it like the list
    // tests below.
    testTf2("tf2_test_dataset.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2TestDataset2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_dataset2.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2TestDataset3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_dataset3.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2TestDataset4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_dataset4.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2TestDataset5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_dataset5.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2TestDataset6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_dataset6.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2TestDataset7()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_dataset7.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2TestDataset8()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_dataset8.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2TestDataset9()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_dataset9.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2TestDataset10()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_dataset10.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2TestTensorList()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_tensor_list.py", "add", 2, 2, 2, 3);
  }

  @Test
  public void testTf2TestTensorList2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_tensor_list2.py", "add", 0, 0);
  }

  @Test
  public void testTf2TestTensorList3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2(
        "tf2_test_tensor_list3.py",
        "add",
        0,
        0); // NOTE: Change to 2, 2, 2, 3 once https://github.com/wala/ML/issues/136 is fixed.
  }

  @Test
  public void testTf2TestTensorList4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_tensor_list4.py", "add", 0, 0);
  }

  @Test
  public void testTf2TestTensorList5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_tensor_list5.py", "add", 0, 0);
  }

  @Test
  public void testTf2TestModelCall()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_model_call.py", "SequentialModel.__call__", 1, 1, 3);
  }

  @Test
  public void testTf2TestModelCall2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_model_call2.py", "SequentialModel.call", 1, 1, 3);
  }

  @Test
  public void testTf2TestModelCall3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_model_call3.py", "SequentialModel.call", 1, 1, 3);
  }

  @Test
  public void testTf2TestModelCall4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_model_call4.py", "SequentialModel.__call__", 1, 1, 3);
  }

  @Test
  public void testTf2TestCallbacks()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_callbacks.py", "replica_fn", 1, 1, 2);
  }

  @Test
  public void testTf2Callbacks()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_callbacks2.py", "replica_fn", 1, 1, 2);
  }

  @Test
  public void testTf2GanTutorial()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tensorflow_gan_tutorial.py", "train_step", 1, 2, 2);
  }

  @Test
  public void testTf2GanTutorial2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tensorflow_gan_tutorial2.py", "train_step", 1, 2, 2);
  }

  @Test
  public void testTf2EagerExecution()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tensorflow_eager_execution.py", "MyModel.call", 1, 1, 3);
  }

  @Test
  public void testTf2NeuralNetwork()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("neural_network.py", "NeuralNet.call", 1, 1, 3);
  }

  @Test
  public void testTf2NeuralNetwork2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2(
        "neural_network.py",
        "cross_entropy_loss",
        1,
        8,
        3); // NOTE: Change to 2 tensor parameters once https://github.com/wala/ML/issues/127 is
    // fixed. Values 2 and 3 will correspond to the tensor parameters.
  }

  @Test
  public void testTf2NeuralNetwork3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("neural_network.py", "run_optimization", 2, 3, 2, 3);
  }

  @Test
  public void testTf2NeuralNetwork4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2(
        "neural_network.py",
        "accuracy",
        1,
        3,
        3); // NOTE: Change to 2 tensor parameters and 5 tensor variables once
    // https://github.com/wala/ML/issues/127 is fixed. Values 2 and 3 will correspond to the
    // tensor parameters.
  }

  @Test
  public void testTf2Autoencoder()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("autoencoder.py", "encoder", 1, 18, 2);
  }

  @Test
  public void testTf2Autoencoder2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("autoencoder.py", "mean_square", 2, 2, 2, 3);
  }

  @Test
  public void testTf2Autoencoder3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("autoencoder.py", "run_optimization", 1, 3, 2);
  }

  @Test
  public void testTf2Autoencoder4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("autoencoder.py", "decoder", 1, 18, 2);
  }

  @Test
  public void testTf2TestSigmoid()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_sigmoid.py", "f", 1, 1, 2);
  }

  @Test
  public void testTf2TestSigmoid2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_sigmoid2.py", "f", 1, 1, 2);
  }

  @Test
  public void testTf2TestAdd()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_add.py", "f", 1, 1, 2);
  }

  @Test
  public void testTf2TestAdd2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_add2.py", "f", 1, 1, 2);
  }

  @Test
  public void testTf2TestAdd3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_add3.py", "f", 1, 1, 2);
  }

  @Test
  public void testTf2TestAdd4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_add4.py", "f", 1, 1, 2);
  }

  @Test
  public void testTf2TestAdd5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_add5.py", "f", 1, 1, 2);
  }

  @Test
  public void testTf2TestAdd6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_add6.py", "f", 1, 1, 2);
  }

  @Test
  public void testTf2MultiGPUTraining()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("multigpu_training.py", "run_optimization", 2, 4, 2, 3);
  }

  @Test
  public void testTf2MultiGPUTraining2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2(
        "multigpu_training.py",
        "average_gradients",
        0,
        0); // NOTE: Change to 1, 1, 2 once https://github.com/wala/ML/issues/136 is fixed.
  }

  @Test
  public void testTf2TestReduceMean()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_reduce_mean.py", "f", 1, 1, 2);
  }

  @Test
  public void testTf2TestReduceMean2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_reduce_mean.py", "g", 1, 1, 2);
  }

  @Test
  public void testTf2TestReduceMean3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_reduce_mean.py", "h", 1, 1, 2);
  }

  @Test
  public void testTf2TestGradient()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_gradient.py", "f", 1, 1, 2);
  }

  @Test
  public void testTf2TestGradient2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_gradient2.py", "f", 1, 1, 2);
  }

  @Test
  public void testTf2TestMultiply()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_multiply.py", "f", 1, 1, 2);
  }

  @Test
  public void testTf2Multiply2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_multiply2.py", "f", 1, 1, 2);
  }

  @Test
  public void testTf2TestSparseSoftmaxCrossEntropyWithLogits()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testTf2("tf2_test_sparse_softmax_cross_entropy_with_logits.py", "f", 1, 1, 2);
  }

  private void testTf2(
      String filename,
      String functionName,
      int expectedNumberOfTensorParameters,
      int expectedNumberOfTensorVariables,
      int... expectedTensorParameterValueNumbers)
      throws ClassHierarchyException, CancelException, IOException {
    PythonAnalysisEngine<TensorTypeAnalysis> E = makeEngine(filename);
    PythonSSAPropagationCallGraphBuilder builder = E.defaultCallGraphBuilder();

    CallGraph CG = builder.makeCallGraph(builder.getOptions());
    assertNotNull(CG);

    if (logger.isLoggable(Level.FINE)) {
      CAstCallGraphUtil.AVOID_DUMP = false;
      CAstCallGraphUtil.dumpCG(
          ((SSAPropagationCallGraphBuilder) builder).getCFAContextInterpreter(),
          builder.getPointerAnalysis(),
          CG);
      logger.fine("Call graph:\n" + CG);
    }

    TensorTypeAnalysis analysis = E.performAnalysis(builder);

    logger.info("Tensor analysis: " + analysis);

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
          } else logger.warning(() -> "Encountered: " + pointerKey.getClass());
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
