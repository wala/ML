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
package com.ibm.wala.cast.python.ipa.summaries;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.strings.Atom;

public class PythonSyntheticClass extends SyntheticClass {
	protected final Map<Selector,PythonSummarizedFunction> functions = HashMapFactory.make();
	protected final Map<Atom,IField> fields = HashMapFactory.make();
	
	public PythonSyntheticClass(TypeReference T, IClassHierarchy cha) {
		super(T, cha);
	}

	@Override
	public IClassLoader getClassLoader() {
	    return getClassHierarchy().getLoader(PythonTypes.pythonLoader);
	}

	@Override
	public boolean isPublic() {
		return true;
	}

	@Override
	public boolean isPrivate() {
		return false;
	}

	@Override
	public int getModifiers() throws UnsupportedOperationException {
		return Constants.ACC_PUBLIC;
	}

	@Override
	public IClass getSuperclass() {
		return getClassHierarchy().lookupClass(PythonTypes.object);
	}

	@Override
	public Collection<? extends IClass> getDirectInterfaces() {
		return Collections.emptySet();
	}

	@Override
	public Collection<IClass> getAllImplementedInterfaces() {
		return Collections.emptySet();
	}

	@Override
	public IMethod getMethod(Selector selector) {
		return functions.get(selector);
	}

	@Override
	public IField getField(Atom name) {
		return fields.get(name);
	}

	@Override
	public IMethod getClassInitializer() {
		return null;
	}

	@Override
	public Collection<PythonSummarizedFunction> getDeclaredMethods() {
		return functions.values();
	}

	@Override
	public Collection<IField> getAllInstanceFields() {
		return fields.values();
	}

	@Override
	public Collection<IField> getAllStaticFields() {
		return Collections.emptySet();
	}

	@Override
	public Collection<IField> getAllFields() {
		return fields.values();
	}

	@Override
	public Collection<PythonSummarizedFunction> getAllMethods() {
		return functions.values();
	}

	@Override
	public Collection<IField> getDeclaredInstanceFields() {
		return fields.values();
	}

	@Override
	public Collection<IField> getDeclaredStaticFields() {
		return Collections.emptySet();
	}

	@Override
	public boolean isReferenceType() {
		return true;
	}

}
