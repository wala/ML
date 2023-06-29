package com.ibm.wala.cast.python.analysis.ap;

/** Marker interface identifying path elements; things like fields, or array contents. */
public interface IPathElement {

  boolean matches(IPathElement other);
}
