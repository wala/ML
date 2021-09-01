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

import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.core.util.strings.Atom;

public class PythonInstanceMethodTrampoline extends PythonSyntheticClass {
	private static final Atom selfName = Atom.findOrCreateUnicodeAtom("self");
	
	public static final FieldReference self = FieldReference.findOrCreate(PythonTypes.Root, selfName, PythonTypes.object);
	
	public static TypeReference findOrCreate(TypeReference cls, IClassHierarchy cha) {
		TypeReference t = trampoline(cls);
		if (cha.lookupClass(t) == null) {
			new PythonInstanceMethodTrampoline(cls, cha);
		}
		return t;
	}
	
	public static TypeReference trampoline(TypeReference x) {
		return TypeReference.findOrCreate(x.getClassLoader(), "L$" + x.getName().toString().substring(1));
	}
	
	private final IClass realClass;
	
	public PythonInstanceMethodTrampoline(TypeReference functionType, IClassHierarchy cha) {
		super(trampoline(functionType), cha);
		realClass = cha.lookupClass(functionType);
		fields.put(selfName, new IField() {
			@Override
			public IClass getDeclaringClass() {
				return PythonInstanceMethodTrampoline.this;
			}

			@Override
			public Atom getName() {
				return selfName;
			}

			@Override
			public Collection<Annotation> getAnnotations() {
				return Collections.emptySet();
			}

			@Override
			public IClassHierarchy getClassHierarchy() {
				return PythonInstanceMethodTrampoline.this.getClassHierarchy();
			}

			@Override
			public TypeReference getFieldTypeReference() {
				return PythonTypes.object;
			}

			@Override
			public FieldReference getReference() {
				return self;
			}

			@Override
			public boolean isFinal() {
				return true;
			}

			@Override
			public boolean isPrivate() {
				return true;
			}

			@Override
			public boolean isProtected() {
				return false;
			}

			@Override
			public boolean isPublic() {
				return false;
			}

			@Override
			public boolean isStatic() {
				return false;
			}

			@Override
			public boolean isVolatile() {
				return false;
			}
		});
		
		cha.addClass(this);
	}

	public String toString() {
		return "Trampoline[" + getReference().getName().toString().substring(1) + "]";
	}

	@Override
	public IClass getSuperclass() {
		return getClassHierarchy().lookupClass(PythonTypes.trampoline);
	}

	public IClass getRealClass() {
		return realClass;
	}
}
