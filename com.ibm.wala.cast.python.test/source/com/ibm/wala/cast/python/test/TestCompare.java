package com.ibm.wala.cast.python.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestAssign extends TestPythonCallGraphShape {

	 protected static final Object[][] assertionsAssign1 = new Object[][] {
		    new Object[] { ROOT, new String[] { "script assign1.py" } },
		    new Object[] {
		        "script assign1.py",
		        new String[] { "script assign1.py/f", "script assign1.py/g" } },
		    new Object[] {
			        "script assign1.py/f",
			        new String[] { "script assign1.py/a" } },
		    new Object[] {
			        "script assign1.py/g",
			        new String[] { "script assign1.py/a" } }
	 };

	@Test
	public void testAssign1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("assign1.py");
		verifyGraphAssertions(CG, assertionsAssign1);
	}

}
