package com.ibm.wala.cast.python.analysis.ap;

/**
 * A path element representing all possible sequences of fields. This should only appear as the
 * final element of an access path.
 */
public class StarPathElement implements IPathElement {

  private static final StarPathElement instance = new StarPathElement();

  public static StarPathElement singleton() {
    return instance;
  }

  private StarPathElement() {}

  @Override
  public String toString() {
    return "*";
  }

  public boolean matches(IPathElement other) {
    return true;
  }
}
