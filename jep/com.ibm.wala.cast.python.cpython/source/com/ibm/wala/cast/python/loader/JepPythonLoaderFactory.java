package com.ibm.wala.cast.python.loader;

import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAOptions;
import java.io.File;
import java.util.List;

public class JepPythonLoaderFactory extends PythonLoaderFactory {

  static {
    System.loadLibrary("jep");
  }

  protected List<File> pythonPath;
  protected SSAOptions ssaOptions;

  public JepPythonLoaderFactory(List<File> pythonPath, SSAOptions ssaOptions) {
    this.pythonPath = pythonPath;
    this.ssaOptions = ssaOptions;
  }

  @Override
  protected IClassLoader makeTheLoader(IClassHierarchy cha) {
    return new JepPythonLoader(cha, ssaOptions);
  }
}
