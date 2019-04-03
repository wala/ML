package com.ibm.wala.cast.python.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestCollections extends TestPythonCallGraphShape {

	@Test
	public void testCalls1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("collections.py");
		System.err.println(CG);
		//verifyGraphAssertions(CG, assertionsCalls1);
	}
	

}
