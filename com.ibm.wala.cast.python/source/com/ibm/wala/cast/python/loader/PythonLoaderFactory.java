/******************************************************************************
 * Copyright (c) 2018 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.python.loader;

import com.ibm.wala.cast.loader.SingleClassLoaderFactory;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;

public abstract class PythonLoaderFactory extends SingleClassLoaderFactory {

	@Override
	public ClassLoaderReference getTheReference() {
		return PythonTypes.pythonLoader;
	}


}
