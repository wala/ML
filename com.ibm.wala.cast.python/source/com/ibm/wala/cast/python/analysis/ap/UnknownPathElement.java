package com.ibm.wala.cast.python.analysis.ap;

/**
 * A path element representing an unknown field. Since the field is unknown,
 * this matches anything.
 * 
 */
public class UnknownPathElement implements IPathElement {
  
  private final static UnknownPathElement instance = new UnknownPathElement();
  
  public static UnknownPathElement singleton() {
    return instance;
  }
  
  private UnknownPathElement() {}

  @Override
  public String toString() {
   return "?";
  }
  
  public boolean matches(IPathElement other) {
   return true;
  }

}
