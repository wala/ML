package com.ibm.cast.python.loader;
import com.ibm.wala.cast.python.loader.PythonLoaderFactory;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.cha.IClassHierarchy;

public class JepPythonLoaderFactory extends PythonLoaderFactory {

	public JepPythonLoaderFactory() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected IClassLoader makeTheLoader(IClassHierarchy cha) {
		return new JepPythonLoader(cha);
	}

}
