package com.ibm.wala.cast.python.analysis.ap;

/** A wildcard path element "[*]" that matches array contents. */
public class ArrayContents implements IPathElement {

  private static final ArrayContents instance = new ArrayContents();

  public static ArrayContents singleton() {
    return instance;
  }

  private ArrayContents() {}

  public boolean matches(IPathElement other) {
    return this.equals(other)
        || other instanceof UnknownPathElement
        || other instanceof StarPathElement;
  }

  public String toString() {
    return "[*]";
  }
}
