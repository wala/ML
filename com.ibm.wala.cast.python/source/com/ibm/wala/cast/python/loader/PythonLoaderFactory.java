package com.ibm.wala.cast.python.loader;

import com.ibm.wala.cast.loader.SingleClassLoaderFactory;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;

public class PythonLoaderFactory extends SingleClassLoaderFactory {

	@Override
	public ClassLoaderReference getTheReference() {
		return PythonTypes.pythonLoader;
	}

	@Override
	protected IClassLoader makeTheLoader(IClassHierarchy cha) {
		return new PythonLoader(cha);
	}

}
