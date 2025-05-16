package com.ibm.wala.cast.python.cpython.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.cast.python.test.TestJythonCallGraphShape;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestFor extends com.ibm.wala.cast.python.test.TestFor {
	
  protected static final Object[][] assertionsFor4 =
		  new Object[][] {
	  new Object[] {ROOT, new String[] {"script for4.py"}},
	  new Object[] {
			  "script for4.py",
			  new String[] {"script for4.py/lambda1"}
	  }
  };

  @Test
  public void testFor4()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
	    CallGraph CG = process("for4.py");
	    verifyGraphAssertions(CG, assertionsFor4);
  }

  protected static final Object[][] assertionsFor5 =
	      new Object[][] {
	        new Object[] {ROOT, new String[] {"script for5.py"}},
	        new Object[] {"script for5.py", new String[] {"script for5.py/doit"}},
	        new Object[] {
	          "script for5.py/doit",
	          new String[] {"script for5.py/lambda1"}
	        }
	      };

  @Test
  public void testFor5()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("for5.py");
    verifyGraphAssertions(CG, assertionsFor5);
  }

  protected static final Object[][] assertionsFor6 =
		  new Object[][] {
	  new Object[] {ROOT, new String[] {"script for6.py"}},
	  new Object[] {
			  "script for6.py",
			  new String[] {"script for6.py/mf/lambda1", "script for6.py/mf"}
	  }
  };

  @Test
  public void testFor6()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("for6.py");
    verifyGraphAssertions(CG, assertionsFor6);
  }

}
