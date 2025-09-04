package com.ibm.wala.cast.python.jep;

import com.ibm.wala.cast.python.util.PythonInterpreter;

public class CPythonInterpreter extends PythonInterpreter {

  @Override
  public Integer evalAsInteger(String expr) {
    return (Integer) Util.runit(expr);
  }
}
