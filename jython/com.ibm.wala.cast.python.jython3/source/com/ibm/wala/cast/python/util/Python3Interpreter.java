package com.ibm.wala.cast.python.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class Python3Interpreter extends com.ibm.wala.cast.python.util.PythonInterpreter {

  private static final Logger LOGGER = Logger.getLogger(Python3Interpreter.class.getName());

  private static PythonInterpreter interp;

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
      } else
        throw new IllegalArgumentException(
            "Python expression: " + expr + " cannot be evaluated to an integer.");
    } catch (PyException e) {
      LOGGER.log(Level.SEVERE, "Unable to interpret Python expression: " + expr, e);
      throw new IllegalArgumentException("Can't interpret Python expression: " + expr + ".", e);
    }
  }
}
