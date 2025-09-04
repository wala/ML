package com.ibm.wala.cast.python.analysis.ap;

import com.ibm.wala.util.collections.Pair;
import java.util.List;
import java.util.Set;

/** A path element representing an instance field. */
public class PropertyPathElement implements IPathElement {

  public static PropertyPathElement createFieldPathElement(String field) {
    return new PropertyPathElement(field);
  }

  public static PropertyPathElement createMethodPathElement(
      String field, final Set<Integer> interestingParameters) {
    class MethodPathElement extends PropertyPathElement implements IMethodAP {

      private MethodPathElement(String field) {
        super(field);
      }

      @Override
      public Set<Integer> getInterestingParameters() {
        return interestingParameters;
      }
    }
    ;

    return new MethodPathElement(field);
  }

  public static IPathElement createCallbackPathElement(
      String field,
      final Set<Pair<Integer, List<String>>> callbackParameters,
      final Set<Pair<Integer, List<String>>> callbackTaintedParams) {
    class CallbackPathElement extends PropertyPathElement implements ICallbackAP {

      private CallbackPathElement(String field) {
        super(field);
      }

      @Override
      public Set<Pair<Integer, List<String>>> getCallbackParameter() {
        return callbackParameters;
      }

      @Override
      public Set<Pair<Integer, List<String>>> getCalleeTaintedParameters() {
        return callbackTaintedParams;
      }

      public boolean equals(Object o) {
        return super.equals(o)
            && (o instanceof CallbackPathElement)
            && ((CallbackPathElement) o).getCallbackParameter().equals(callbackParameters)
            && ((CallbackPathElement) o).getCalleeTaintedParameters().equals(callbackTaintedParams);
      }

      public String toString() {
        return super.toString();
      }
    }

    return new CallbackPathElement(field);
  }

  private final String field;

  private PropertyPathElement(String field) {
    this.field = field;
  }
  ;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((field == null) ? 0 : field.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof PropertyPathElement)) {
      return false;
    }

    PropertyPathElement other = (PropertyPathElement) obj;

    if (field == null) {
      if (other.field != null) return false;
    } else if (!field.equals(other.field)) return false;

    return true;
  }

  public boolean matches(IPathElement other) {
    return this.equals(other)
        || other instanceof UnknownPathElement
        || other instanceof StarPathElement;
  }

  @Override
  public String toString() {
    return field.toString();
  }
}
