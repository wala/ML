package com.ibm.wala.cast.python.cpython.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.cast.python.test.TestJythonCallGraphShape;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestWalrus extends TestJythonCallGraphShape {

	protected static final Object[][] assertionsWalrus1 =
      new Object[][] {
		new Object[] {ROOT, new String[] {"script walrus1.py"}},
		new Object[] {
		  "script walrus1.py",
		  new String[] {
	        "$script walrus1.py/Foo/f:trampoline2", "script walrus1.py/g", "$script walrus1.py/Foo/f:trampoline2"
		  }
		},
		new Object[] {
			"script walrus1.py/g",
			new String[] { "script walrus1.py/Foo" }
		},
		new Object[] {
			"script walrus1.py/Foo",
			new String[] { "script walrus1.py/Foo/__init__" }
		},
		new Object[] {
			"$script walrus1.py/Foo/f:trampoline2",
			new String[] { "script walrus1.py/Foo/f" }
		},
		new Object[] {
			"$script walrus1.py/Foo/h:trampoline2",
			new String[] { "script walrus1.py/Foo/h" }
		}
	};
		 
	@Test
	public void testWalrus1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
	    CallGraph CG = process("walrus1.py");
	    verifyGraphAssertions(CG, assertionsWalrus1);
	}
}
