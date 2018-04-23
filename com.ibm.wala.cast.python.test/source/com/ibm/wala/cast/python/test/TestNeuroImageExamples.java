package com.ibm.wala.cast.python.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestNeuroImageExamples extends TestPythonCallGraphShape {

	private static final String Ex1URL = "https://raw.githubusercontent.com/corticometrics/neuroimage-tensorflow/master/train.py";
	
	@Test
	public void testEx1CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process(Ex1URL);
		System.err.println(CG);
	}
	
	private static final String Ex2URL = "http://nilearn.github.io/_downloads/plot_group_level_connectivity.py";
	
	@Test
	public void testEx2CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process(Ex2URL);
		System.err.println(CG);
	}	
}
