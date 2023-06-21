package com.ibm.wala.cast.python.loader;

import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.cha.IClassHierarchy;

public class PytestLoaderFactory extends PythonLoaderFactory {

  @Override
  protected IClassLoader makeTheLoader(IClassHierarchy cha) {
    return new PytestLoader(cha);
  }
}
