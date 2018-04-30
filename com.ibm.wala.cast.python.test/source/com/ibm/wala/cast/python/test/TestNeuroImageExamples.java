package com.ibm.wala.cast.python.test;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import org.junit.Test;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.client.PythonTensorAnalysisEngine;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestNeuroImageExamples extends TestPythonCallGraphShape {

	private static final String Ex1URL = "https://raw.githubusercontent.com/corticometrics/neuroimage-tensorflow/master/train.py";
	
	@Test
	public void testEx1CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonTensorAnalysisEngine e = new PythonTensorAnalysisEngine();
		e.setModuleFiles(Collections.singleton(new SourceURLModule(new URL(Ex1URL))));
		PropagationCallGraphBuilder cgBuilder = (PropagationCallGraphBuilder) e.defaultCallGraphBuilder();
		CallGraph CG = cgBuilder.getCallGraph();	
		CAstCallGraphUtil.AVOID_DUMP = false;
		CAstCallGraphUtil.dumpCG((SSAContextInterpreter)cgBuilder.getContextInterpreter(), cgBuilder.getPointerAnalysis(), CG);
		TensorTypeAnalysis result = e.performAnalysis(cgBuilder);

		String in = "[{[D:Constant,64000] of pixel}]";
		String out = "[{[D:Constant,40, D:Constant,40, D:Constant,40, D:Constant,1] of pixel}]";
		checkReshape(cgBuilder, CG, result, in, out);
		System.err.println(result);
	}
	
	private static final String Ex2URL = "http://nilearn.github.io/_downloads/plot_group_level_connectivity.py";
	
	@Test
	public void testEx2CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process(Ex2URL);
		System.err.println(CG);
	}	
	
	public static final String Ex3URL = "https://raw.githubusercontent.com/zsdonghao/u-net-brain-tumor/master/train.py";

	@Test
	public void testEx3CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process(Ex3URL);
		System.err.println(CG);
	}	
	
	public static final String Ex4URL = "https://raw.githubusercontent.com/zsdonghao/u-net-brain-tumor/master/prepare_data_with_valid.py";

	@Test
	public void testEx4CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process(Ex4URL);
		System.err.println(CG);
	}	
}
