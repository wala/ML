package com.ibm.wala.cast.python.cpython.test;

import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.wala.cast.python.test.*;

@RunWith(Suite.class)
@SuiteClasses({
	RunJep.class,
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
	TestGenerators.class})
public class PassingSuite {

	public static void main(String... args ) {
		JUnitCore.runClasses(PassingSuite.class);
	}
	
}
