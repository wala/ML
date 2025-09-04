package com.ibm.wala.cast.python.analysis.ap;

import com.ibm.wala.util.collections.Pair;
import java.util.List;
import java.util.Set;

public class GlobalCallbackAP extends GlobalVarAP implements ICallbackAP {
  private final Set<Pair<Integer, List<String>>> taintedParametersInCallback;
  private final Set<Pair<Integer, List<String>>> callbackParameters;

  public GlobalCallbackAP(
      String varName,
      Set<Pair<Integer, List<String>>> callbackParameters,
      Set<Pair<Integer, List<String>>> taintedParametersInCallback) {
    super(varName);
    this.taintedParametersInCallback = taintedParametersInCallback;
    this.callbackParameters = callbackParameters;
  }

  @Override
  public Set<Pair<Integer, List<String>>> getCallbackParameter() {
    return callbackParameters;
  }

  @Override
  public Set<Pair<Integer, List<String>>> getCalleeTaintedParameters() {
    return taintedParametersInCallback;
  }
}
