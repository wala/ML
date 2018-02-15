package com.ibm.wala.cast.python.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestTensorflowModel extends TestPythonCallGraphShape {

	@Test
	public void testPf1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		System.err.println(process("tf1.py").fst);
	}

}
