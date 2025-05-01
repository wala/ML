package com.ibm.wala.cast.python.cpython.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.wala.cast.python.test.*;

@RunWith(Suite.class)
@SuiteClasses({
	TestCalls.class,
	TestClasses.class,
	TestCallables.class,
	TestCompare.class,
	TestAssign.class})
public class PassingSuite {

}
