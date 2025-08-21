package com.ibm.wala.cast.python.ipa.summaries;

import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import java.util.Map;

public abstract class PythonTrampolines implements MethodTargetSelector {

  protected final MethodTargetSelector base;
  private final Map<IClass, PythonSummarizedFunction> trampolines = HashMapFactory.make();

  public PythonTrampolines(MethodTargetSelector base) {
    super();
    this.base = base;
  }

  abstract PythonSummarizedFunction makeTrampoline(
      CGNode caller, CallSiteReference site, IClass receiver, MethodReference method);

  @Override
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    MethodReference method = site.getDeclaredTarget();
    if (method.getSelector().equals(AstMethodReference.fnSelector)
        && caller
            .getClassHierarchy()
            .isSubclassOf(receiver, caller.getClassHierarchy().lookupClass(specializedType()))
        && !trampolines.values().contains(caller.getMethod())) {

      if (trampolines.containsKey(receiver)) {
        return trampolines.get(receiver);

      } else {

        PythonSummarizedFunction code = makeTrampoline(caller, site, receiver, method);

        trampolines.put(receiver, code);

        return code;
      }
    }

    return base.getCalleeTarget(caller, site, receiver);
  }

  protected abstract TypeReference specializedType();
}
