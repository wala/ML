package com.ibm.wala.cast.python.analysis.ap;

public class GlobalVarAP implements IAPRoot {
    
  public static GlobalVarAP createGlobalVarAP(String varName) {
    return new GlobalVarAP(varName);
  }

  private final String varName;
  
  public String getVarName() {
    return varName;
  }

  protected GlobalVarAP(String varName) {
    this.varName = varName;
  }

  
  public Kind getKind() {
    return Kind.GLOBAL;
  }

  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((varName == null) ? 0 : varName.hashCode());
    return result;
  }

  
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (! (obj instanceof GlobalVarAP))
      return false;
    GlobalVarAP other = (GlobalVarAP) obj;
    if (varName == null) {
      if (other.varName != null)
        return false;
    } else if (!varName.equals(other.varName))
      return false;
    return true;
  }

  
  public int length() {
    return 1;
  }

  
  @Override
  public String toString() {
    return varName.toString();
  }

}
