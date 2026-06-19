package com.ibm.wala.cast.python.loader;

import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAOptions;
import java.util.List;

public class Python2LoaderFactory extends PythonLoaderFactory {

  private SSAOptions ssaOptions;

  public Python2LoaderFactory(List<?> path, SSAOptions ssaOptions) {
    this.ssaOptions = ssaOptions;
  }

  @Override
  protected IClassLoader makeTheLoader(IClassHierarchy cha) {
    return new Python2Loader(cha, ssaOptions);
  }
}
