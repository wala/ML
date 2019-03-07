package com.ibm.wala.cast.python.analysis.ap;

/**
 * Marker interface identifying access paths.
 * 
 * @author sjfink
 *
 */
public interface IAccessPath {
  
  public enum Kind {
    LEXICAL, LOCAL, LIST, GLOBAL, CALLBACK
  }
  
  public Kind getKind();

  public int length();
}
