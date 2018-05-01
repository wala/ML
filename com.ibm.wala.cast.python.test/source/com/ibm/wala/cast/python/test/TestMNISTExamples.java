package com.ibm.wala.cast.python.test;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

import com.ibm.wala.cast.python.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.analysis.TensorVariable;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.ssa.PythonPropertyWrite;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.python.types.TensorType;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;

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
	
	public void testMnistSoftmax(String url) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		Set<SSAInstruction> goodCalls = HashSetFactory.make();
		checkTensorOps(url, (PropagationCallGraphBuilder cgBuilder, CallGraph CG, TensorTypeAnalysis result) -> {
			Set<CGNode> nodes = CG.getNodes(MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, "Ltensorflow/functions/Runner"), AstMethodReference.fnSelector));
			assert nodes.size() > 0;
			for(CGNode node : nodes) {
				for(Iterator<CGNode> callers = CG.getPredNodes(node); callers.hasNext(); ) {
					CGNode caller = callers.next();
					for(Iterator<CallSiteReference> sites = CG.getPossibleSites(caller, node); sites.hasNext(); ) {
						for(SSAAbstractInvokeInstruction call : caller.getIR().getCalls(sites.next())) {
							int dictVn = ((PythonInvokeInstruction)call).getUse("feed_dict");
							assert dictVn != -1;
							caller.getDU().getUses(dictVn).forEachRemaining((SSAInstruction use) -> {
								if (use instanceof PythonPropertyWrite && ((PythonPropertyWrite)use).getObjectRef()==dictVn) {
									int varVn = ((PythonPropertyWrite)use).getMemberRef();
									int dataVn = ((PythonPropertyWrite)use).getValue();
									PointsToSetVariable var = cgBuilder.getPropagationSystem().findOrCreatePointsToSet(cgBuilder.getPointerKeyFactory().getPointerKeyForLocal(caller, varVn));
									PointsToSetVariable data = cgBuilder.getPropagationSystem().findOrCreatePointsToSet(cgBuilder.getPointerKeyFactory().getPointerKeyForLocal(caller, dataVn));
									TensorVariable v = result.getOut(var);
									TensorVariable d = result.getOut(data);
									if (! v.getTypes().isEmpty() && ! d.getTypes().isEmpty()) {
										for(TensorType vt : v.getTypes()) {
											for(TensorType dt : d.getTypes()) {
												assert vt.concreteSize() == dt.concreteSize();
												assert vt.symbolicDims() == dt.symbolicDims();
												goodCalls.add(call);
												System.err.println(call + " looks good for " + vt + " and " + dt);
											}
										}
									}	
								}
							});
						}
					}
				}
			}
		});
		assert !goodCalls.isEmpty();
	}

	@Test
	public void testEx3CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		testMnistSoftmax(Ex3URL);
	}
	
	private static final String Ex4URL = "https://raw.githubusercontent.com/tensorflow/tensorflow/master/tensorflow/examples/tutorials/mnist/mnist_softmax_xla.py";

	@Test
	public void testEx4CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		testMnistSoftmax(Ex4URL);
	}

	private static final String Ex5URL = "https://raw.githubusercontent.com/tensorflow/tensorflow/master/tensorflow/examples/tutorials/mnist/mnist_with_summaries.py";

	@Test
	public void testEx5CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process(Ex5URL);
		System.err.println(CG);
	}
}
