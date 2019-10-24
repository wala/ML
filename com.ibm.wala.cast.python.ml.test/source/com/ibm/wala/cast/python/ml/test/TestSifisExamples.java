package com.ibm.wala.cast.python.ml.test;

import java.io.IOException;

import org.junit.Test;
import org.junit.Ignore;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestSifisExamples extends TestPythonMLCallGraphShape {

	private static final String Ex1URL = "https://raw.githubusercontent.com/ForeverZyh/TensorFlow-Program-Bugs/master/StackOverflow/UT-1/38167455-buggy/mnist.py";

	protected static final Object[][] assertionsEx1 = new Object[][] {
	    new Object[] { ROOT, new String[] { "script mnist.py" } },
	};
	
	@Ignore
	@Test
	public void testEx1CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process(Ex1URL);
		verifyGraphAssertions(CG, assertionsEx1);
	}

	private static final String Ex2URL = "https://raw.githubusercontent.com/ForeverZyh/TensorFlow-Program-Bugs/master/StackOverflow/UT-13/42191656-buggy/linear.py";

	protected static final Object[][] assertionsEx2 = new Object[][] {
	    new Object[] { ROOT, new String[] { "script linear.py" } },
	};
	
	@Test
	public void testEx2CG() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process(Ex2URL);
		verifyGraphAssertions(CG, assertionsEx2);
	}
}
