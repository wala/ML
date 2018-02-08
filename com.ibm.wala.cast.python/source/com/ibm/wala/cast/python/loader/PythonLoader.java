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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.python.core.PyObject;

import com.ibm.wala.cast.ir.translator.AstTranslator.AstLexicalInformation;
import com.ibm.wala.cast.ir.translator.AstTranslator.WalkContext;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.ir.translator.TranslatorToIR;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.loader.CAstAbstractModuleLoader;
import com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.parser.PythonModuleParser;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.CAstTypeDictionaryImpl;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.strings.Atom;

public class PythonLoader extends CAstAbstractModuleLoader {
	private final CAstTypeDictionaryImpl<PyObject> typeDictionary = new CAstTypeDictionaryImpl<PyObject>();
	
	public PythonLoader(IClassHierarchy cha, IClassLoader parent) {
		super(cha, parent);
	}

	public PythonLoader(IClassHierarchy cha) {
		super(cha);
	}

	@Override
	public ClassLoaderReference getReference() {
		return PythonTypes.pythonLoader;
	}

	@Override
	public Language getLanguage() {
		return PythonLanguage.Python;
	}

	@Override
	public SSAInstructionFactory getInstructionFactory() {
		return getLanguage().instructionFactory();
	}

	@Override
	protected TranslatorToCAst getTranslatorToCAst(CAst ast, ModuleEntry M) throws IOException {
		return new PythonModuleParser((SourceURLModule)M, typeDictionary);
	}

	@Override
	protected boolean shouldTranslate(CAstEntity entity) {
		return true;
	}

	@Override
	protected TranslatorToIR initTranslator() {
		return new PythonCAstToIRTranslator(this);
	}

	CoreClass Root = new CoreClass(PythonTypes.rootTypeName, null, this, null);

	CoreClass Exception = new CoreClass(PythonTypes.Exception.getName(), PythonTypes.rootTypeName, this, null);

	CoreClass CodeBody = new CoreClass(PythonTypes.CodeBody.getName(), PythonTypes.rootTypeName, this, null);

	CoreClass object = new CoreClass(PythonTypes.object.getName(), PythonTypes.rootTypeName, this, null);

	public IClass makeCodeBodyType(String name, TypeReference P, CAstSourcePositionMap.Position sourcePosition, CAstEntity entity, WalkContext context) {
		return new DynamicCodeBody(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName(name)), P, this,
				sourcePosition, entity, context);
	}

	public IClass defineFunctionType(String name, CAstSourcePositionMap.Position pos, CAstEntity entity, WalkContext context) {
		return makeCodeBodyType(name, PythonTypes.CodeBody, pos, entity, context);
	}

	public IMethod defineCodeBodyCode(String clsName, AbstractCFG<?, ?> cfg, SymbolTable symtab, boolean hasCatchBlock,
			Map<IBasicBlock<SSAInstruction>, TypeReference[]> caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
		DynamicCodeBody C = (DynamicCodeBody) lookupClass(clsName, cha);
		assert C != null : clsName;
		return C.setCodeBody(makeCodeBodyCode(cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp, lexicalInfo, debugInfo, C));
	}

	public DynamicMethodObject makeCodeBodyCode(AbstractCFG<?, ?> cfg, SymbolTable symtab, boolean hasCatchBlock,
			Map<IBasicBlock<SSAInstruction>, TypeReference[]> caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo,
			IClass C) {
		return new DynamicMethodObject(C, Collections.emptySet(), cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp, lexicalInfo,
				debugInfo);
	}

	class PythonClass extends CoreClass {
		private java.util.Set<IField> staticFields = HashSetFactory.make();
		
		public PythonClass(TypeName name, TypeName superName, IClassLoader loader, Position sourcePosition) {
			super(name, superName, loader, sourcePosition);
		}

		@Override
		public Collection<IField> getDeclaredStaticFields() {
			return staticFields;
		}	
	}
	
	public void defineType(TypeName cls, TypeName parent) {
		new PythonClass(cls, parent, this, null);
	}
	
	public void defineField(TypeName cls, CAstEntity field) {
		assert types.containsKey(cls);
		((PythonClass)types.get(cls)).staticFields.add(new IField() {
			@Override
			public String toString() {
				return "field:" + getName();
			}
			
			@Override
			public IClass getDeclaringClass() {
				return types.get(cls);
			}

			@Override
			public Atom getName() {
				return Atom.findOrCreateUnicodeAtom(field.getName());
			}

			@Override
			public Collection<Annotation> getAnnotations() {
				return Collections.emptySet();
			}

			@Override
			public IClassHierarchy getClassHierarchy() {
				return cha;
			}

			@Override
			public TypeReference getFieldTypeReference() {
				return PythonTypes.Root;
			}

			@Override
			public FieldReference getReference() {
				return FieldReference.findOrCreate(getDeclaringClass().getReference(), getName(), getFieldTypeReference());
			}

			@Override
			public boolean isFinal() {
				return false;
			}

			@Override
			public boolean isPrivate() {
				return false;
			}

			@Override
			public boolean isProtected() {
				return false;
			}

			@Override
			public boolean isPublic() {
				return true;
			}

			@Override
			public boolean isStatic() {
				return true;
			}

			@Override
			public boolean isVolatile() {
				return false;
			}			
		});
	}
}
