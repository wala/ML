package com.ibm.wala.cast.python.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestClasses extends TestPythonCallGraphShape {

	@Test
	public void testClasses1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("classes1.py");
		System.err.println(CG);
		CG.forEach((n) -> {
			System.err.println(n.getIR());
		});
	}

}
