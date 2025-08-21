package com.ibm.wala.cast.python.jep;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.function.Supplier;
import jep.Interpreter;
import jep.JepException;
import jep.SharedInterpreter;
import jep.python.PyObject;

public class Util {
  private static Interpreter interp;

  private static final class JepQueue extends Thread {

    @Override
    public void run() {
      interp = new SharedInterpreter();
      interp.exec("import ast");
      interp.exec("import ast2json");
      interp.exec("has = hasattr");
      interp.exec("get_value = lambda x: eval(compile(ast.Expression(x), '', 'eval'))");

      while (true) {
        try {
          operations.take().run();
        } catch (InterruptedException e) {
          assert false : e;
        }
      }
    }

    public <T> Future<T> addTask(Function<Interpreter, T> op) {
      FutureTask<T> t =
          new FutureTask<T>(
              new Callable<T>() {
                @Override
                public T call() throws Exception {
                  return op.apply(interp);
                }
              });
      operations.add(t);
      return t;
    }
  }

  //
  // the JEP api provides a JNI wrapper for calling Python from Java.
  //
  // JEP is not thread safe, and using @ThreadLocal is a way to
  // ensure that any one JEP interpreter is only ever accessed from
  // a single thread
  //
  static BlockingQueue<FutureTask<?>> operations = new LinkedBlockingQueue<>();
  static JepQueue queueThread;

  static {
    queueThread = new JepQueue();

    queueThread.setDaemon(true);
    queueThread.start();
  }

  public static <T> T runWithJep(Supplier<T> job) {
    try {
      return queueThread
          .addTask(
              new Function<Interpreter, T>() {
                @Override
                public T apply(Interpreter interp) {
                  return job.get();
                }
              })
          .get();
    } catch (InterruptedException | ExecutionException e) {
      assert false : e;
      return null;
    }
  }

  public static String typeName(PyObject obj) {
    assert Thread.currentThread() == queueThread;
    interp.set("obj", obj);
    interp.exec("objname = type(obj).__name__");

    return (String) interp.getValue("objname");
  }

  public static String moduleName(PyObject obj) {
    assert Thread.currentThread() == queueThread;
    interp.set("obj", obj);
    interp.exec("modname = type(obj).__module__");

    return (String) interp.getValue("modname");
  }

  public static <T> T runit(String expr) {
    assert Thread.currentThread() == queueThread;
    interp.exec("result = " + expr);

    return (T) interp.getValue("result");
  }

  /**
   * parse Python code into the standard CPython AST
   *
   * @param code source code to be parsed into an AST
   * @return AST as a @PyObject
   */
  public static PyObject getAST(String code) {
    assert Thread.currentThread() == queueThread;
    interp.set("code", code);
    interp.exec("theast = ast.parse(code)");

    return (PyObject) interp.getValue("theast");
  }

  /**
   * turn an AST into a JSON representation
   *
   * @param ast a Python AST as a @PyObject
   * @return JSON form of the AST as tree of @Map objects
   */
  @SuppressWarnings("unchecked")
  public static Map<String, ?> getJSON(PyObject ast) {
    assert Thread.currentThread() == queueThread;
    interp.set("theast", ast);
    interp.exec("thejson = ast2json.ast2json(theast)");

    return (Map<String, ?>) interp.getValue("thejson");
  }

  public static PyObject fixForCompilation(PyObject ast) {
    try {
      assert Thread.currentThread() == queueThread;
      return (PyObject) interp.invoke("ast.fix_missing_locations", ast);
    } catch (JepException e) {
      return null;
    }
  }

  public static boolean has(PyObject o, String property) {
    assert Thread.currentThread() == queueThread;
    return (Boolean) interp.invoke("has", o, property);
  }

  public static PyObject compile(PyObject o) {
    assert Thread.currentThread() == queueThread;
    PyObject exprNode = (PyObject) interp.invoke("ast.Expression", o);
    return (PyObject) interp.invoke("compile_ast", exprNode, "", "eval");
  }

  public static Object run(PyObject o) {
    try {
      assert Thread.currentThread() == queueThread;
      return interp.invoke("get_value", o);
    } catch (JepException e) {
      return null;
    }
  }
}
