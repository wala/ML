package com.ibm.wala.cast.python.loader;

import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import java.io.File;
import java.util.List;

public class PytestLoaderFactory extends PythonLoaderFactory {

  /**
   * The <a
   * href="https://docs.python.org/3/tutorial/modules.html#the-module-search-path">PYTHONPATH</a> to
   * use in the analysis.
   *
   * @apiNote PYTHONPATH is currently only supported for Python 3.
   */
  protected List<File> pythonPath;

  public PytestLoaderFactory(List<File> pythonPath) {
    this.pythonPath = pythonPath;
  }

  @Override
  protected IClassLoader makeTheLoader(IClassHierarchy cha) {
    return new PytestLoader(cha, this.getPythonPath());
  }

  /**
   * Gets the <a
   * href="https://docs.python.org/3/tutorial/modules.html#the-module-search-path">PYTHONPATH</a> to
   * use in the analysis.
   *
   * @apiNote PYTHONPATH is currently only supported for Python 3.
   */
  public List<File> getPythonPath() {
    return pythonPath;
  }
}
