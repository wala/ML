package com.ibm.wala.cast.python.analysis.ap;

/**
 * Access path representing a lexical variable.
 */
public class LexicalAP implements IAPRoot {

  public static LexicalAP createLexicalAP(String name, String definer) {
    return new LexicalAP(name, definer);
  }

  private final String name;
  private final String definer;

  private LexicalAP(String name, String definer) {
    this.name = name;
    this.definer = definer;
  }

  public String getName() {
    return name;
  }

  public String getDefiner() {
    return definer;
  }

  public Kind getKind() {
    return Kind.LEXICAL;
  }

  public int length() {
    return 1;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + definer.hashCode();
    result = prime * result + name.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    LexicalAP other = (LexicalAP) obj;
    if (!definer.equals(other.definer))
      return false;
    if (!name.equals(other.name))
      return false;
    return true;
  }

  public String toString() {
    return "(" + name + "," + definer + ")";
    
  }
}
