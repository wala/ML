package com.ibm.wala.cast.python.ml.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestPandasModel extends TestPythonMLCallGraphShape {

	@Test
	public void testPandas1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("pandas1.py");
		System.err.println(CG);
	}

}
