package com.ibm.wala.cast.python.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.cast.python.analysis.TensorTypeAnalysis;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestMNISTExamples extends TestPythonCallGraphShape {

	private static final String Ex1URL = "https://raw.githubusercontent.com/aymericdamien/TensorFlow-Examples/dd2e6dcd9603d5de008d8c766453162d0204affa/examples/3_NeuralNetworks/convolutional_network.py";

	 protected static final Object[][] assertionsEx1 = new Object[][] {
		    new Object[] {
		        "model_fn",
		        new String[] { "conv_net" } }
	 };

	@Test
	public void testEx1CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process(Ex1URL);
		System.err.println(CG);
		verifyGraphAssertions(CG, assertionsEx1);
	}
	
	@Test
	public void testEx1Tensors() throws IllegalArgumentException, CancelException, IOException {
		checkTensorOps(Ex1URL, (PropagationCallGraphBuilder cgBuilder, CallGraph CG, TensorTypeAnalysis result) -> {
			String in = "[{[D:Symbolic,n, D:Compound,[D:Constant,28, D:Constant,28]] of pixel}]";
			String out = "[{[D:Symbolic,?, D:Constant,28, D:Constant,28, D:Constant,1] of pixel}]";
			checkTensorOp(cgBuilder, CG, result, "reshape", in, out);		

			in = "[{[D:Symbolic,?, D:Constant,28, D:Constant,28, D:Constant,1] of pixel}]";
			checkTensorOp(cgBuilder, CG, result, "conv2d", in, null);
		});
	}

	private static final String Ex2URL = "https://raw.githubusercontent.com/tensorflow/tensorflow/master/tensorflow/examples/tutorials/mnist/mnist_deep.py";

	@Test
	public void testEx2CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process(Ex2URL);
		System.err.println(CG);
	}

	@Test
	public void testEx2Tensors() throws IllegalArgumentException, CancelException, IOException {
		checkTensorOps(Ex2URL, (PropagationCallGraphBuilder cgBuilder, CallGraph CG, TensorTypeAnalysis result) -> {
			String in = "[{[D:Symbolic,?, D:Constant,784] of pixel}]";
			String out = "[{[D:Symbolic,?, D:Constant,28, D:Constant,28, D:Constant,1] of pixel}]";
			checkTensorOp(cgBuilder, CG, result, "reshape", in, out);

			in = "[{[D:Symbolic,?, D:Constant,28, D:Constant,28, D:Constant,1] of pixel}]";
			checkTensorOp(cgBuilder, CG, result, "conv2d", in, null);
		});
	}
	
	private static final String Ex3URL = "https://raw.githubusercontent.com/tensorflow/tensorflow/master/tensorflow/examples/tutorials/mnist/mnist_softmax.py";
	
	@Test
	public void testEx3CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process(Ex3URL);
		System.err.println(CG);
	}

	private static final String Ex4URL = "https://raw.githubusercontent.com/tensorflow/tensorflow/master/tensorflow/examples/tutorials/mnist/mnist_softmax_xla.py";

	@Test
	public void testEx4CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process(Ex4URL);
		System.err.println(CG);
	}

	private static final String Ex5URL = "https://raw.githubusercontent.com/tensorflow/tensorflow/master/tensorflow/examples/tutorials/mnist/mnist_with_summaries.py";

	@Test
	public void testEx5CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process(Ex5URL);
		System.err.println(CG);
	}
}
