package com.ibm.wala.cast.python.jython.test;

import com.ibm.wala.cast.python.test.TestAssign;
import com.ibm.wala.cast.python.test.TestCalls;
import com.ibm.wala.cast.python.test.TestClasses;
import com.ibm.wala.cast.python.test.TestCollections;
import com.ibm.wala.cast.python.test.TestComprehension;
import com.ibm.wala.cast.python.test.TestFor;
import com.ibm.wala.cast.python.test.TestLambda;
import com.ibm.wala.cast.python.test.TestLibrary;
import com.ibm.wala.cast.python.test.TestMulti;
import com.ibm.wala.cast.python.test.TestPrimitives;
import com.ibm.wala.cast.python.test.TestSlice;
import com.ibm.wala.cast.python.test.TestSource;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  TestAssign.class,
  TestCalls.class,
  TestClasses.class,
  TestCollections.class,
  TestComprehension.class,
  TestFor.class,
  TestLambda.class,
  TestLibrary.class,
  TestMulti.class,
  TestPrimitives.class,
  TestSlice.class,
  TestSource.class
})
public class Jython2Suite {}
