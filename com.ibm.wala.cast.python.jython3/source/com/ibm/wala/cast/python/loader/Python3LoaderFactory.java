package com.ibm.wala.cast.python.loader;

import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import java.io.File;
import java.util.List;

public class Python3LoaderFactory extends PythonLoaderFactory {

  /**
   * The <a href="https://docs.python.org/3/using/cmdline.html#envvar-PYTHONPATH">PYTHONPATH</a> to
   * use in the analysis.
   *
   * @apiNote PYTHONPATH is currently only supported for Python 3.
   * @see https://docs.python.org/3/tutorial/modules.html#the-module-search-path.
   */
  protected List<File> pythonPath;

  public Python3LoaderFactory(List<File> pythonPath) {
    this.pythonPath = pythonPath;
  }

  @Override
  protected IClassLoader makeTheLoader(IClassHierarchy cha) {
    return new Python3Loader(cha, this.getPythonPath());
  }

  /**
   * Gets the <a
   * href="https://docs.python.org/3/using/cmdline.html#envvar-PYTHONPATH">PYTHONPATH</a> to use in
   * the analysis.
   *
   * @apiNote PYTHONPATH is currently only supported for Python 3.
   * @see https://docs.python.org/3/tutorial/modules.html#the-module-search-path.
   */
  public List<File> getPythonPath() {
    return pythonPath;
  }
}
