package com.ibm.wala.cast.python.ml.test;

import com.ibm.wala.cast.python.ml.analysis.TensorTypeAnalysis;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import org.junit.Ignore;
import org.junit.Test;

public class TestNeuroImageExamples extends TestPythonMLCallGraphShape {

  private static final String Ex1URL =
      "https://raw.githubusercontent.com/corticometrics/neuroimage-tensorflow/master/train.py";

  @Test
  public void testEx1CG()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    checkTensorOps(
        Ex1URL,
        (PropagationCallGraphBuilder cgBuilder, CallGraph CG, TensorTypeAnalysis result) -> {
          String in = "[{[D:Constant,64000] of pixel}]";
          String out = "[{[D:Constant,40, D:Constant,40, D:Constant,40, D:Constant,1] of pixel}]";
          // NOTE: Change last two arguments to `in`, `out` once
          // https://github.com/wala/ML/issues/195 is fixed.
          checkTensorOp(cgBuilder, CG, result, "reshape", null, null);

          in = "[{[D:Constant,40, D:Constant,40, D:Constant,40, D:Constant,1] of pixel}]";
          // NOTE: Change next to last argument to `in` once https://github.com/wala/ML/issues/195
          // is
          // fixed.
          checkTensorOp(cgBuilder, CG, result, "conv3d", null, null);
        });
  }

  private static final String Ex2URL =
      "https://raw.githubusercontent.com/nilearn/nilearn/refs/heads/main/examples/03_connectivity/plot_group_level_connectivity.py";

  @Test
  public void testEx2CG()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process(Ex2URL);
    System.err.println(CG);
  }

  public static final String Ex3URL =
      "https://raw.githubusercontent.com/zsdonghao/u-net-brain-tumor/master/train.py";

  @Test
  public void testEx3CG()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process(Ex3URL);
    System.err.println(CG);
  }

  public static final String Ex4URL =
      "https://raw.githubusercontent.com/zsdonghao/u-net-brain-tumor/master/prepare_data_with_valid.py";

  @Ignore
  @Test
  public void testEx4CG()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process(Ex4URL);
    System.err.println(CG);
  }
}
