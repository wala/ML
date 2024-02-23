package com.ibm.wala.cast.python.types;

import com.ibm.wala.types.TypeName;

public class Util {

  /**
   * Returns the filename portion of the given {@link TypeName} representing a Python type.
   *
   * @param typeName A {@link TypeName} of a Python type.
   * @return The filename portion of the given {@link TypeName}.
   * @apiNote Python types include a file in their {@link TypeName}s in Ariadne.
   */
  public static String getFilename(final TypeName typeName) {
    String ret = typeName.toString();
    ret = ret.substring("Lscript ".length());

    if (ret.indexOf('/') != -1) ret = ret.substring(0, ret.indexOf('/'));

    return ret;
  }

  private Util() {}
}
