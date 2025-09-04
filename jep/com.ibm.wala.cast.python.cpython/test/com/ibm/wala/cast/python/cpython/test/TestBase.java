package com.ibm.wala.cast.python.cpython.test;

import java.util.List;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class TestBase {

  protected static boolean run(Class<?>... cls) {
    Result result = JUnitCore.runClasses(cls);
    int rc = result.getRunCount();
    System.err.println(rc + " test" + (rc > 1 ? "s" : "") + " run");
    List<Failure> failures = result.getFailures();
    if (!failures.isEmpty()) {
      int fc = result.getFailureCount();
      System.err.println(fc + " test" + (fc > 1 ? "s" : "") + " failed");
      failures.forEach(
          f -> {
            System.err.println(f.getDescription());
            System.err.println(f.getException());
          });
      return false;
    } else {
      return true;
    }
  }
}
