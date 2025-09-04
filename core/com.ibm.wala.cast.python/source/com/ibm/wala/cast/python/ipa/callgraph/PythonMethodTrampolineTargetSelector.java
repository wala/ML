package com.ibm.wala.cast.python.ipa.callgraph;

import com.ibm.wala.cast.python.ipa.summaries.PythonSummarizedFunction;
import com.ibm.wala.cast.python.ipa.summaries.PythonSummary;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import java.util.Map;
import java.util.logging.Logger;

public abstract class PythonMethodTrampolineTargetSelector<T> implements MethodTargetSelector {

  protected final MethodTargetSelector base;

  protected final Map<Pair<IClass, Integer>, IMethod> codeBodies = HashMapFactory.make();

  public PythonMethodTrampolineTargetSelector(MethodTargetSelector base) {
    this.base = base;
  }

  @Override
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    if (receiver != null) {
      Logger logger = this.getLogger();

      logger.fine("Getting callee target for receiver: " + receiver);
      logger.fine("Calling method name is: " + caller.getMethod().getName());

      if (this.shouldProcess(caller, site, receiver)) {
        PythonInvokeInstruction call = this.getCall(caller, site);
        Pair<IClass, Integer> key = this.makeKey(receiver, call);

        if (!codeBodies.containsKey(key)) {
          MethodReference tr =
              MethodReference.findOrCreate(
                  receiver.getReference(),
                  Atom.findOrCreateUnicodeAtom("trampoline" + call.getNumberOfTotalParameters()),
                  AstMethodReference.fnDesc);
          PythonSummary x = new PythonSummary(tr, call.getNumberOfTotalParameters());
          int v = call.getNumberOfTotalParameters() + 1;

          populate(x, v, receiver, call, logger);

          codeBodies.put(key, new PythonSummarizedFunction(tr, x, receiver));
        }

        return codeBodies.get(key);
      }
    }

    return base.getCalleeTarget(caller, site, receiver);
  }

  /**
   * Returns the {@link PythonInvokeInstruction} at the given {@link CallSiteReference} within the
   * given {@link CGNode}.
   *
   * @param caller The calling {@link CGNode}.
   * @param site A {@link CallSiteReference} within the given {@link CGNode}.
   * @return The {@link PythonInvokeInstruction} at the given {@link CallSiteReference} within the
   *     given {@link CGNode}.
   */
  protected PythonInvokeInstruction getCall(CGNode caller, CallSiteReference site) {
    return (PythonInvokeInstruction) caller.getIR().getCalls(site)[0];
  }

  /**
   * Returns a unique {@link Pair} for the given {@link Receiver} and {@link
   * PythonInvokeInstruction}.
   *
   * @return A unique {@link Pair} for the given {@link Receiver} and {@link
   *     PythonInvokeInstruction}.
   */
  private Pair<IClass, Integer> makeKey(IClass receiver, PythonInvokeInstruction call) {
    return Pair.make(receiver, call.getNumberOfTotalParameters());
  }

  /**
   * The {@link Logger} to be used.
   *
   * @return The {@link Logger} to be used.
   */
  protected abstract Logger getLogger();

  /**
   * True iff this {@link PythonMethodTrampolineTargetSelector} should handle the given {@link
   * CGNode}, {@link CallSiteReference}, {@link IClass} combination. If the combination is not to be
   * processed, the next target selector will be used.
   *
   * @return True iff this {@link PythonMethodTrampolineTargetSelector} should handle the given
   *     {@link CGNode}, {@link CallSiteReference}, {@link IClass} combination.
   */
  protected abstract boolean shouldProcess(CGNode caller, CallSiteReference site, IClass receiver);

  /**
   * Populate the given {@link PythonSummary} that will be used as the trampoline. At the completion
   * of this method, the given {@link PythonInvokeInstruction} will be the last instruction.
   *
   * <p>This fill the trampoline body that eventually invokes the original method.
   *
   * @param x The {@link PythonSummary} representing the trampoline to fill.
   * @param v The starting variable number in the SSA.
   * @param receiver The receiver of the original call.
   * @param call The original call.
   * @param logger The {@link Logger} to use.
   */
  protected abstract void populate(
      PythonSummary x, int v, IClass receiver, PythonInvokeInstruction call, Logger logger);
}
