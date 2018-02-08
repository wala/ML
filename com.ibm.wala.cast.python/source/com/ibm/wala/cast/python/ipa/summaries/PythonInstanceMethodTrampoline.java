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

import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.strings.Atom;

public class PythonInstanceMethodTrampoline extends PythonSyntheticClass {
	private static final Atom selfName = Atom.findOrCreateUnicodeAtom("self");

	private static final Atom initName = Atom.findOrCreateUnicodeAtom("$init");

	private static final Descriptor initDescr = Descriptor.findOrCreate(new TypeName[] { PythonTypes.Root.getName() }, PythonTypes.Root.getName());
	
	public static final FieldReference self = FieldReference.findOrCreate(PythonTypes.Root, selfName, PythonTypes.object);
	
	public static final MethodReference init = MethodReference.findOrCreate(PythonTypes.Root, initName, initDescr);
	
	public PythonInstanceMethodTrampoline(TypeReference functionType, IClassHierarchy cha) {
		super(functionType, cha);
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
		
		PythonSummary ctor = new PythonSummary(init, 1);
		SSAInstructionFactory insts = PythonLanguage.Python.instructionFactory();
		ctor.addStatement(insts.PutInstruction(0, 1, 2, self));
		
		functions.put(init.getSelector(), new PythonSummarizedFunction(init, ctor, this));
	}

}
