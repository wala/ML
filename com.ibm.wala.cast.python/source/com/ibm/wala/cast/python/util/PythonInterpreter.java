package com.ibm.wala.cast.python.util;

public abstract class PythonInterpreter {

  static {
    try {
      @SuppressWarnings("unchecked")
      Class<PythonInterpreter> i4 =
          (Class<PythonInterpreter>)
              Class.forName("com.ibm.wala.cast.python.jep.CPythonInterpreter");
      setInterpreter(i4.newInstance());
    } catch (ClassNotFoundException
        | InstantiationException
        | IllegalAccessException
        | UnsatisfiedLinkError e2) {
      try {
        Class<?> i3 = Class.forName("com.ibm.wala.cast.python.util.Python3Interpreter");
        setInterpreter((PythonInterpreter) i3.newInstance());
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
        try {
          Class<?> i2 = Class.forName("com.ibm.wala.cast.python.util.Python2Interpreter");
          setInterpreter((PythonInterpreter) i2.newInstance());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e1) {
          assert false : e.getMessage() + ", then " + e1.getMessage();
        }
      }
    }
  }

  public abstract Integer evalAsInteger(String expr);

  private static PythonInterpreter interp;

  public static void setInterpreter(PythonInterpreter interpreter) {
    interp = interpreter;
  }

  public static Integer interpretAsInt(String expr) {
    return interp.evalAsInteger(expr);
  }
}
