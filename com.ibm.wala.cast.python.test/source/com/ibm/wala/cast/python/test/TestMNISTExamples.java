package com.ibm.wala.cast.python.test;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

import com.ibm.wala.cast.python.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.analysis.TensorVariable;
import com.ibm.wala.cast.python.client.PythonTensorAnalysisEngine;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
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
		verifyGraphAssertions(CG, assertionsEx1);
	}
	
	@Test
	public void testEx1Tensors() throws IllegalArgumentException, CancelException, IOException {
		PythonTensorAnalysisEngine e = new PythonTensorAnalysisEngine();
		e.setModuleFiles(Collections.singleton(new SourceURLModule(new URL(Ex1URL))));
		PropagationCallGraphBuilder cgBuilder = (PropagationCallGraphBuilder) e.defaultCallGraphBuilder();
		CallGraph CG = cgBuilder.getCallGraph();		
		TensorTypeAnalysis result = e.performAnalysis(cgBuilder);

		Set<CGNode> reshapes = CG.getNodes(MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, "Ltensorflow/functions/reshape"), AstMethodReference.fnSelector));
		assert reshapes.size() > 0;
		for(CGNode reshape : reshapes) {
			for(Iterator<CGNode> callers = CG.getPredNodes(reshape); callers.hasNext(); ) {
				CGNode caller = callers.next();
				for(Iterator<CallSiteReference> sites = CG.getPossibleSites(caller, reshape); sites.hasNext(); ) {
					for(SSAAbstractInvokeInstruction call : caller.getIR().getCalls(sites.next())) {
						TensorVariable orig = result.getOut(cgBuilder.getPropagationSystem().findOrCreatePointsToSet(cgBuilder.getPointerKeyForLocal(caller, call.getUse(1))));
						assert "tensor types:[{[D:Symbolic,n, D:Compound,[D:Constant,28, D:Constant,28]] of pixel}]".equals(orig.toString());
						
						TensorVariable reshaped = result.getOut(cgBuilder.getPropagationSystem().findOrCreatePointsToSet(cgBuilder.getPointerKeyForLocal(caller, call.getDef())));
						assert "tensor types:[{[D:Symbolic,?, D:Constant,28, D:Constant,28, D:Constant,1] of pixel}]".equals(reshaped.toString());
					}
				}
			}
		}
	}
	
	private static final String Ex2URL = "https://raw.githubusercontent.com/tensorflow/tensorflow/master/tensorflow/examples/tutorials/mnist/mnist_deep.py";

	@Test
	public void testEx2CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process(Ex2URL);
	}

}
