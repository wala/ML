package com.ibm.wala.cast.python.util;

import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class Python2Interpreter extends com.ibm.wala.cast.python.util.PythonInterpreter {

  private static PythonInterpreter interp = null;

  public static PythonInterpreter getInterp() {
    if (interp == null) {
      PySystemState.initialize();
      interp = new PythonInterpreter();
    }
    return interp;
  }

  public Integer evalAsInteger(String expr) {
    try {
      PyObject val = getInterp().eval(expr);
      if (val.isInteger()) {
        return val.asInt();
      }
    } catch (PyException e) {

    }

    return null;
  }
}
