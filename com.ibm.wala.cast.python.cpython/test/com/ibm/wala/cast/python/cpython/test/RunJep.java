package com.ibm.wala.cast.python.cpython.test;

import org.junit.Test;

import com.ibm.wala.cast.python.jep.Util;

public class RunJep {

	@Test
	public void runJep() {
		Util.run(() -> {
			assert Util.getAST("1 + a") != null;
			return null;
		});
	}
	
}
