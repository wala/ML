package com.ibm.wala.cast.python.ml.test;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.ml.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.ml.analysis.TensorVariable;
import com.ibm.wala.cast.python.ml.types.TensorType;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.ssa.PythonPropertyWrite;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.PropagationSystem;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;

public class TestMNISTExamples extends TestPythonMLCallGraphShape {

	private static final String Ex1URL = "https://raw.githubusercontent.com/aymericdamien/TensorFlow-Examples/dd2e6dcd9603d5de008d8c766453162d0204affa/examples/3_NeuralNetworks/convolutional_network.py";

	 protected static final Object[][] assertionsEx1 = new Object[][] {
		    new Object[] {
		        "script convolutional_network.py/model_fn",
		        new String[] { "script convolutional_network.py/conv_net" } }
	 };

	@Test
	public void testEx1CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process(Ex1URL);
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
	}

	@Test
	public void testEx2Tensors() throws IllegalArgumentException, CancelException, IOException {
		checkTensorOps(Ex2URL, (PropagationCallGraphBuilder cgBuilder, CallGraph CG, TensorTypeAnalysis result) -> {
			CAstCallGraphUtil.AVOID_DUMP = false;
			CAstCallGraphUtil.dumpCG((SSAContextInterpreter) cgBuilder.getContextInterpreter(), cgBuilder.getPointerAnalysis(), CG);
			
			String in = "[{[D:Symbolic,?, D:Constant,784] of pixel}]";
			String out = "[{[D:Symbolic,?, D:Constant,28, D:Constant,28, D:Constant,1] of pixel}]";
			checkTensorOp(cgBuilder, CG, result, "reshape", in, out);

			in = "[{[D:Symbolic,?, D:Constant,28, D:Constant,28, D:Constant,1] of pixel}]";
			checkTensorOp(cgBuilder, CG, result, "conv2d", in, null);

			Set<SSAInstruction> goodFeeds = HashSetFactory.make();
			checkDirectFeeddict(goodFeeds, cgBuilder, CG, result);
			assert goodFeeds.size() > 0;
		});
	}
	
	private static final String Ex3URL = "https://raw.githubusercontent.com/tensorflow/tensorflow/master/tensorflow/examples/tutorials/mnist/mnist_softmax.py";
	
	private void testMnistSoftmax(String url) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		Set<SSAInstruction> goodCalls = HashSetFactory.make();
		checkTensorOps(url, (PropagationCallGraphBuilder cgBuilder, CallGraph CG, TensorTypeAnalysis result) -> {
			checkDirectFeeddict(goodCalls, cgBuilder, CG, result);
		});
		assert !goodCalls.isEmpty();
	}

	private void checkDirectFeeddict(Set<SSAInstruction> goodCalls, PropagationCallGraphBuilder cgBuilder, CallGraph CG,
			TensorTypeAnalysis result) {
		for(CGNode node : CG) {
			cgBuilder.getContextInterpreter().iterateCallSites(node).forEachRemaining((CallSiteReference site) -> { 
				for(SSAAbstractInvokeInstruction call : node.getIR().getCalls(site)) {
					if (call instanceof PythonInvokeInstruction) {
						int dictVn = ((PythonInvokeInstruction)call).getUse("feed_dict");
						if (dictVn != -1) {
							checkFeedDictObject(goodCalls, cgBuilder, result, node, call, dictVn);
						}
					}
				}
			});
		}
	}

	private void checkFeedDictObject(Set<SSAInstruction> goodInstrs, 
			PropagationCallGraphBuilder cgBuilder,
			TensorTypeAnalysis result, 
			CGNode caller, 
			SSAInstruction root, 
			int dictVn) {
		caller.getDU().getUses(dictVn).forEachRemaining((SSAInstruction use) -> {
			if (use instanceof PythonPropertyWrite && ((PythonPropertyWrite)use).getObjectRef()==dictVn) {
				int varVn = ((PythonPropertyWrite)use).getMemberRef();
				int dataVn = ((PythonPropertyWrite)use).getValue();
				PointerKeyFactory keys = cgBuilder.getPointerKeyFactory();
				PointerKey varKey = keys.getPointerKeyForLocal(caller, varVn);
				PointerKey dataKey = keys.getPointerKeyForLocal(caller, dataVn);
				PropagationSystem system = cgBuilder.getPropagationSystem();
				if (!system.isImplicit(dataKey) && !system.isImplicit(varKey)) {
					PointsToSetVariable var = system.findOrCreatePointsToSet(varKey);
					PointsToSetVariable data = system.findOrCreatePointsToSet(dataKey);
					TensorVariable v = result.getOut(var);
					TensorVariable d = result.getOut(data);
					if (! v.getTypes().isEmpty() && ! d.getTypes().isEmpty()) {
						for(TensorType vt : v.getTypes()) {
							for(TensorType dt : d.getTypes()) {
								assert vt.concreteSize() == dt.concreteSize();
								assert vt.symbolicDims() == dt.symbolicDims();
								goodInstrs.add(root);
								System.err.println(root + " looks good for " + vt + " and " + dt);
							}
						}
					}	
				}
			}
		});
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
		checkTensorOps(Ex5URL, (PropagationCallGraphBuilder cgBuilder, CallGraph CG, TensorTypeAnalysis result) -> {
			String in = "[{[D:Symbolic,?, D:Constant,784] of pixel}]";
			String out = "[{[D:Symbolic,?, D:Constant,28, D:Constant,28, D:Constant,1] of pixel}]";
			checkTensorOp(cgBuilder, CG, result, "reshape", in, out);
			
			TypeReference feedDictClass = TypeReference.findOrCreate(PythonTypes.pythonLoader, "Lscript mnist_with_summaries.py/train/feed_dict");
			MethodReference feedDictCode = MethodReference.findOrCreate(feedDictClass, AstMethodReference.fnSelector);
			Set<CGNode> feedDictNodes = CG.getNodes(feedDictCode);
			Set<SSAInstruction> goodFeeds = HashSetFactory.make();
			assert feedDictNodes.size() > 0;
			for(CGNode fd : feedDictNodes) {
				for(SSAInstruction inst : fd.getIR().getInstructions()) {
					if (inst instanceof SSAReturnInstruction) {
						checkFeedDictObject(goodFeeds, cgBuilder, result, fd, inst, inst.getUse(0));
					}
				}
			}
			assert goodFeeds.size() > 0;
		});
	}
}
