package com.ibm.wala.cast.python.cpython.test;

import com.ibm.wala.cast.python.test.TestCallables;
import com.ibm.wala.cast.python.test.TestCalls;
import com.ibm.wala.cast.python.test.TestClasses;
import com.ibm.wala.cast.python.test.TestCompare;
import com.ibm.wala.cast.python.test.TestLambda;
import com.ibm.wala.cast.python.test.TestLibrary;
import com.ibm.wala.cast.python.test.TestMulti;
import com.ibm.wala.cast.python.test.TestPrint;
import com.ibm.wala.cast.python.test.TestTry;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
  TestFString.class,
  TestAsync.class,
  TestRaise.class,
  TestWalrus.class,
  TestFor.class,
  TestComprehension.class,
  TestTry.class,
  TestLibrary.class,
  TestMulti.class,
  TestPrint.class,
  TestLambda.class,
  TestCalls.class,
  TestClasses.class,
  TestCallables.class,
  TestCompare.class,
  TestAssign.class,
  TestGenerators.class,
  TestMatch.class
})
public class PassingSuite extends TestBase {

  public static void main(String... args) {
    assert run(PassingSuite.class);
  }
}
