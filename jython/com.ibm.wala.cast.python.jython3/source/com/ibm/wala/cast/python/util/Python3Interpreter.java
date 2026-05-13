package com.ibm.wala.cast.python.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class Python3Interpreter extends com.ibm.wala.cast.python.util.PythonInterpreter {

  private static final Logger LOGGER = Logger.getLogger(Python3Interpreter.class.getName());

  private static PythonInterpreter interp;

  /**
   * Memoizes a failed Jython init so subsequent {@link #getInterp()} calls return {@code null}
   * cheaply instead of re-running {@code new PythonInterpreter()} (which can be expensive when it
   * fails — site-import walks the Jython resource path on every attempt). When a single failure has
   * occurred, callers receive {@code null} and can degrade their behavior (e.g., {@link
   * com.ibm.wala.cast.python.loader.Python3Loader} skips constant folding).
   */
  private static volatile boolean initFailed = false;

  /**
   * Memoizes whether the "interpreter unavailable" warning has already been emitted from {@link
   * #evalAsInteger(String)}. Without this, callers like {@code interpretAsInt} (invoked many times
   * during shape inference) would flood logs with one WARNING per call after the first init
   * failure. The first failure is already announced by the catch block in {@link #getInterp()};
   * subsequent calls log at FINE level only.
   *
   * @implNote Uses {@link AtomicBoolean#compareAndSet} so the check-and-set is atomic. Under
   *     concurrent call graph construction, multiple threads can race into the {@code if (ip ==
   *     null)} branch simultaneously; a non-atomic {@code volatile boolean} flag would let several
   *     of them all pass the check before any sets it, defeating the "log once" intent.
   */
  private static final AtomicBoolean unavailableWarned = new AtomicBoolean(false);

  public static synchronized PythonInterpreter getInterp() {
    if (initFailed) return null;
    if (interp == null) {
      try {
        PySystemState.initialize();
        interp = new PythonInterpreter();
      } catch (Exception t) {
        // Jython init can fail when bootstrap resources (e.g., the embedded
        // _frozen_importlib bytecode used by org.python.core.imp) aren't reachable from the
        // current classloader/working directory. This is environment-dependent (e.g., happens
        // under Tycho-OSGi but not under plain Maven-surefire). Treat as a recoverable failure:
        // log once, memoize, and let callers degrade gracefully.
        //
        // We catch {@link Exception} (not {@link Throwable}) so that {@link Error} types
        // (OOM, stack overflow, linkage errors) keep propagating to the caller — those signal
        // serious VM problems we don't want to silently swallow and continue past.
        initFailed = true;
        LOGGER.log(
            Level.WARNING,
            t,
            () ->
                "Jython interpreter init failed; all interpreter-based evaluation will be disabled"
                    + " for this run (constant folding in Python3Loader, shape-argument"
                    + " evaluation via interpretAsInt/evalAsInteger, etc.).");
        return null;
      }
    }
    return interp;
  }

  public Integer evalAsInteger(String expr) {
    PythonInterpreter ip = getInterp();
    if (ip == null) {
      // Return {@code null} (the same "cannot evaluate" signal used elsewhere in this method's
      // contract) rather than throwing, so callers like
      // {@code com.ibm.wala.cast.python.util.PythonInterpreter#interpretAsInt} — which expect a
      // nullable {@link Integer} and don't catch checked or runtime exceptions — degrade
      // gracefully in the same OSGi-classloader environments that triggered the {@link
      // #getInterp()} init failure in the first place.
      //
      // Log the first such call at WARNING (so operators see that some shape inference is being
      // skipped because of the earlier init failure); subsequent calls log at FINE only, since
      // the underlying init failure has already been announced from {@link #getInterp()}.
      if (unavailableWarned.compareAndSet(false, true)) {
        LOGGER.log(
            Level.WARNING,
            () ->
                "Jython interpreter unavailable (init failed earlier); evalAsInteger will return"
                    + " null for this and any subsequent calls. First skipped expression: "
                    + expr);
      } else {
        LOGGER.log(Level.FINE, () -> "evalAsInteger returning null (interp unavailable): " + expr);
      }
      return null;
    }
    try {
      PyObject val = ip.eval(expr);
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
