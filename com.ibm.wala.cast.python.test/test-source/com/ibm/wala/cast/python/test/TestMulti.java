package com.ibm.wala.cast.python.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestMulti extends TestPythonCallGraphShape {

	protected static final Object[][] assertionsCalls1 = new Object[][] {
		new Object[] { ROOT, new String[] { "script calls1.py", "script calls2.py" } }
	};

	@Test
	public void testCalls1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("calls1.py", "calls2.py");
		verifyGraphAssertions(CG, assertionsCalls1);
	}
	
}
