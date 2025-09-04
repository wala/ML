package com.ibm.wala.cast.python.cpython.test;

import com.ibm.wala.cast.python.test.TestJythonCallGraphShape;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import org.junit.Test;

public class TestFString extends TestJythonCallGraphShape {

  protected static final Object[][] assertionsForFString1 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script f-strings1.py"}},
        new Object[] {
          "script f-strings1.py", new String[] {"script f-strings1.py/g", "script f-strings1.py/f"}
        },
        new Object[] {"script f-strings1.py/g", new String[] {"script f-strings1.py/f"}}
      };

  @Test
  public void testFString1()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("f-strings1.py");
    verifyGraphAssertions(CG, assertionsForFString1);
  }

  protected static final Object[][] assertionsForFString2 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script f-strings2.py"}},
        new Object[] {"script f-strings2.py", new String[] {"script f-strings2.py/g"}},
        new Object[] {
          "script f-strings2.py/g",
          new String[] {"script f-strings2.py/g", "script f-strings2.py/f"}
        }
      };

  @Test
  public void testFString2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("f-strings2.py");
    verifyGraphAssertions(CG, assertionsForFString2);
  }
}
