package com.ibm.wala.cast.python.cpython.test;

import com.ibm.wala.cast.python.jep.Util;
import org.junit.Test;

public class RunJep extends TestBase {

  @Test
  public void runJep() {
    Util.runWithJep(
        () -> {
          assert Util.getAST("1 + a") != null;
          return null;
        });
  }

  public static void main(String... args) {
    assert run(RunJep.class);
  }
}
