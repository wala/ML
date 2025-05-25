package com.ibm.wala.cast.python.cpython.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.cast.python.test.TestJythonCallGraphShape;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestAnnotations extends TestJythonCallGraphShape {
		 
	@Test
	public void testAnnotations3() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
	    CallGraph CG = process("annotations3.py");
	    //verifyGraphAssertions(CG, assertionsWalrus1);
	}
}
