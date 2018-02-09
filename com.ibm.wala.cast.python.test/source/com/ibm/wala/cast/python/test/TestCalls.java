package com.ibm.wala.cast.python.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestCalls extends TestPythonCallGraphShape {

	@Test
	public void testCalls1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		System.err.println(process("calls1.py").fst);
	}

	@Test
	public void testCalls2() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		System.err.println(process("calls2.py").fst);
	}
	
	@Test
	public void testCalls3() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		System.err.println(process("calls3.py").fst);
	}
	
	@Test
	public void testCalls4() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		System.err.println(process("calls4.py").fst);
	}
	

}
