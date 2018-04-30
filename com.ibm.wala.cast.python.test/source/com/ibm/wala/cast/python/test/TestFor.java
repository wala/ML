package com.ibm.wala.cast.python.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestFor extends TestPythonCallGraphShape {

	 protected static final Object[][] assertionsFor1 = new Object[][] {
		    new Object[] { ROOT, new String[] { "script for1.py" } },
		    new Object[] {
		        "script for1.py",
		        new String[] { "f1", "f2", "f3" } }
	 };

	 @Test
	public void testFor1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("for1.py");
		verifyGraphAssertions(CG, assertionsFor1);
	}

	 protected static final Object[][] assertionsFor2 = new Object[][] {
		    new Object[] { ROOT, new String[] { "script for2.py" } },
		    new Object[] {
			        "script for2.py",
			        new String[] { "doit" } },
		    new Object[] {
		        "doit",
		        new String[] { "f1", "f2", "f3" } }
	 };
	
	@Test
	public void testFor2() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("for2.py");
		verifyGraphAssertions(CG, assertionsFor2);
	}

	 protected static final Object[][] assertionsComp1 = new Object[][] {
		    new Object[] { ROOT, new String[] { "script comp1.py" } },
		    new Object[] {
		        "script comp1.py",
		        new String[] { "f1", "f2", "f3" } }
	 };

	 @Test
	public void testComp1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("comp1.py");
		verifyGraphAssertions(CG, assertionsComp1);
	}

	@Test
	public void testComp2() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("comp2.py");
		System.err.println(CG);
	}

}
