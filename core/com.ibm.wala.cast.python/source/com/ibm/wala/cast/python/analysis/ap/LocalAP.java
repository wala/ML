package com.ibm.wala.cast.python.analysis.ap;

/**
 * An access path representing a local variable
 *
 * @author sjfink
 */
public class LocalAP implements IAPRoot {

  // special value number which indicates the return value from a method.
  public static final int RETURN_VALUE_NUMBER = -1;

  public static LocalAP createLocalAP(int valueNumber) {
    return new LocalAP(valueNumber);
  }

  private final int valueNumber;

  public int getValueNumber() {
    return valueNumber;
  }

  private LocalAP(int valueNumber) {
    this.valueNumber = valueNumber;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + valueNumber;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    LocalAP other = (LocalAP) obj;
    if (valueNumber != other.valueNumber) return false;
    return true;
  }

  public Kind getKind() {
    return Kind.LOCAL;
  }

  public int length() {
    return 1;
  }

  public String toString() {
    if (valueNumber == RETURN_VALUE_NUMBER) {
      return "ret";
    }
    return "v" + valueNumber;
  }
}
