package com.ibm.wala.cast.python.loader;

import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import java.io.File;
import java.util.List;

public class JepPythonLoaderFactory extends PythonLoaderFactory {

  static {
    System.loadLibrary("jep");
  }

  protected List<File> pythonPath;

  public JepPythonLoaderFactory(List<File> pythonPath) {
    this.pythonPath = pythonPath;
  }

  @Override
  protected IClassLoader makeTheLoader(IClassHierarchy cha) {
    return new JepPythonLoader(cha);
  }
}
