package com.ibm.wala.cast.python.test;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import org.junit.Test;

public class TestTry extends TestPythonCallGraphShape {

  protected static final Object[][] assertionsTry =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script try.py"}},
        new Object[] {
          "script try.py",
          new String[] {"script try.py/test1", "script try.py/test2", "script try.py/test3"}
        },
        new Object[] {
          "script try.py/test1",
          new String[] {
            "script try.py/test1/f1", "script try.py/test1/f2", "script try.py/test1/f3"
          }
        },
        new Object[] {
          "script try.py/test2",
          new String[] {
            "script try.py/test2/f1", "script try.py/test2/f2", "script try.py/test2/f3"
          }
        },
        new Object[] {
          "script try.py/test3",
          new String[] {
            "script try.py/test3/f1", "script try.py/test3/f2", "script try.py/test3/f3"
          }
        },
      };

  @Test
  public void testTry()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("try.py");
    System.err.println(CG);
    verifyGraphAssertions(CG, assertionsTry);
  }
}
