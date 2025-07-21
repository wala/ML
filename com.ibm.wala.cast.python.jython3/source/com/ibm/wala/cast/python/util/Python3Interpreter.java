package com.ibm.wala.cast.python.util;

import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class Python3Interpreter extends com.ibm.wala.cast.python.util.PythonInterpreter {

  private static PythonInterpreter interp;

  public static PythonInterpreter getInterp() {
    if (interp == null) {
      PySystemState.initialize();
      interp = new PythonInterpreter();
    }
    return interp;
  }

  public Integer evalAsInteger(String expr) {
    PyObject val = getInterp().eval(expr);
    if (val.isInteger()) {
      return val.asInt();
    } else {
      return null;
    }
  }
}
