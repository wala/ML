package com.ibm.wala.cast.python.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestFor extends TestJythonCallGraphShape {

  protected static final Object[][] assertionsFor1 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script for1.py"}},
        new Object[] {
          "script for1.py",
          new String[] {"script for1.py/f1", "script for1.py/f2", "script for1.py/f3"}
        }
      };

  @Test
  public void testFor1()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("for1.py");
    verifyGraphAssertions(CG, assertionsFor1);
  }

  protected static final Object[][] assertionsFor2 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script for2.py"}},
        new Object[] {"script for2.py", new String[] {"script for2.py/doit"}},
        new Object[] {
          "script for2.py/doit",
          new String[] {"script for2.py/f1", "script for2.py/f2", "script for2.py/f3"}
        }
      };

  @Test
  public void testFor2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("for2.py");
    verifyGraphAssertions(CG, assertionsFor2);
  }

  protected static final Object[][] assertionsFor3 =
		  new Object[][] {
	  new Object[] {ROOT, new String[] {"script for3.py"}},
	  new Object[] {
			  "script for3.py",
			  new String[] {"script for3.py/g1", "script for3.py/g2", "script for3.py/g3"}
	  },
	  new Object[] {
			  "script for3.py/g1",
			  new String[] {"script for3.py/f1", "script for3.py/f2", "script for3.py/f3"}
	  },
	  new Object[] {
			  "script for3.py/g2",
			  new String[] {"script for3.py/f1", "script for3.py/f2", "script for3.py/f3"}
	  },
	  new Object[] {
			  "script for3.py/g3",
			  new String[] {"script for3.py/f1", "script for3.py/f2", "script for3.py/f3"}
	  },
  };

  @Test
  public void testFor3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("for3.py");
    verifyGraphAssertions(CG, assertionsFor3);
  }

  protected static final Object[][] assertionsFor4 =
		  new Object[][] {
	  new Object[] {ROOT, new String[] {"script for4.py"}},
	  new Object[] {
			  "script for4.py",
			  new String[] {"script for4.py/lambda1"}
	  }
  };

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
