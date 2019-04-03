package com.ibm.wala.cast.python.util;

import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class PythonUtil {

	private static PythonInterpreter interp = null;
	
	public static PythonInterpreter getInterp() {
		if (interp == null) {
			PySystemState.initialize(  );
			interp = new PythonInterpreter(  );
		}
		return interp;
	}

}
