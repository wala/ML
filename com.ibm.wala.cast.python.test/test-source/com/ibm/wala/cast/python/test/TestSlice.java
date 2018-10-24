package com.ibm.wala.cast.python.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestSlice extends TestPythonCallGraphShape {

	@Test
	public void testSlice1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("slice1.py");
		System.err.println(CG);
		CG.forEach((CGNode x) -> {
			System.err.println(x.getIR());
		});
		//verifyGraphAssertions(CG, assertionsCalls1);
	}
	

}
