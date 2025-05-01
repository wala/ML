package com.ibm.wala.cast.python.cpython.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.wala.cast.python.test.TestAssign;
import com.ibm.wala.cast.python.test.TestCallables;
import com.ibm.wala.cast.python.test.TestClasses;
import com.ibm.wala.cast.python.test.TestCompare;

@RunWith(Suite.class)
@SuiteClasses({
	TestClasses.class,
	TestCallables.class,
	TestCompare.class,
	TestAssign.class})
public class PassingSuite {

}
