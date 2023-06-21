package com.ibm.wala.cast.python.test;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import java.io.IOException;
import org.junit.Test;

public class TestLambda extends TestPythonCallGraphShape {

  protected static final Object[][] assertionsLambda1 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script lambda1.py"}},
        new Object[] {
          "script lambda1.py",
          new String[] {
            "script lambda1.py/Foo",
            "$script lambda1.py/Foo/foo:trampoline3",
            "script lambda1.py/lambda1",
            "script lambda1.py/lambda2"
          }
        },
        new Object[] {
          "$script lambda1.py/Foo/foo:trampoline3", new String[] {"script lambda1.py/Foo/foo"}
        }
      };

  @Test
  public void testLambda1()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("lambda1.py");
    verifyGraphAssertions(CG, assertionsLambda1);
  }

  protected static final Object[][] assertionsLambda2 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script lambda2.py"}},
        new Object[] {
          "script lambda2.py",
          new String[] {
            "script lambda2.py/Foo",
            "$script lambda2.py/Foo/foo:trampoline3",
            "script lambda2.py/lambda2",
            "script lambda2.py/lambda3"
          }
        },
        new Object[] {
          "$script lambda2.py/Foo/foo:trampoline3", new String[] {"script lambda2.py/Foo/foo"}
        },
        new Object[] {"script lambda2.py/lambda2", new String[] {"script lambda2.py/lambda1"}},
        new Object[] {"script lambda2.py/lambda3", new String[] {"script lambda2.py/lambda1"}}
      };

  @Test
  public void testLambda2()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("lambda2.py");
    verifyGraphAssertions(CG, assertionsLambda2);
  }

  protected static final Object[][] assertionsLambda3 =
      new Object[][] {
        new Object[] {ROOT, new String[] {"script lambda3.py"}},
        new Object[] {
          "script lambda3.py",
          new String[] {"script lambda3.py/Foo", "$script lambda3.py/Foo/foo:trampoline3"}
        },
        new Object[] {
          "$script lambda3.py/Foo/foo:trampoline3", new String[] {"script lambda3.py/Foo/foo"}
        },
        new Object[] {"script lambda3.py/Foo/foo", new String[] {"script lambda3.py/lambda3"}},
        new Object[] {
          "script lambda3.py/lambda3",
          new String[] {"script lambda3.py/lambda1", "script lambda3.py/lambda2"}
        }
      };

  @Test
  public void testLambda3()
      throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    CallGraph CG = process("lambda3.py");
    System.err.println(CG);
    verifyGraphAssertions(CG, assertionsLambda3);
  }
}
