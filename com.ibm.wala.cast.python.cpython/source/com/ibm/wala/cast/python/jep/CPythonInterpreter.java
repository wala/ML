package com.ibm.wala.cast.python.jep;

import com.ibm.wala.cast.python.util.PythonInterpreter;

import jep.Interpreter;

public class CPythonInterpreter extends PythonInterpreter {

	@Override
	public Integer evalAsInteger(String expr) {
		Interpreter interp = Util.interps.get();
		
		interp.exec("i = " + expr);

		return (Integer) interp.getValue("i");

	}

}
