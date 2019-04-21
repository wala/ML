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
import com.ibm.wala.cast.ir.translator.ConstantFoldingRewriter;
import com.ibm.wala.cast.ir.translator.RewritingTranslatorToCAst;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.ir.translator.TranslatorToIR;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.loader.CAstAbstractModuleLoader;
import com.ibm.wala.cast.python.ir.PythonCAstToIRTranslator;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.parser.PythonModuleParser;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.python.util.PythonUtil;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.impl.CAstTypeDictionaryImpl;
import com.ibm.wala.cast.tree.rewrite.AstConstantFolder;
import com.ibm.wala.cast.tree.rewrite.CAstBasicRewriter.NoKey;
import com.ibm.wala.cast.tree.rewrite.CAstBasicRewriter.NonCopyingContext;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.cast.tree.rewrite.PatternBasedRewriter;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cast.util.CAstPattern;
import com.ibm.wala.cast.util.CAstPattern.Segments;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.strings.Atom;

public class PythonLoader extends CAstAbstractModuleLoader {
	private final CAstTypeDictionaryImpl<String> typeDictionary = new CAstTypeDictionaryImpl<String>();
	
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

	private final CAst Ast = new CAstImpl();
	
	private final CAstPattern sliceAssign = CAstPattern.parse("<top>ASSIGN(CALL(VAR(\"slice\"),<args>**),<value>*)");

	private final CAstPattern sliceAssignOp = CAstPattern.parse("<top>ASSIGN_POST_OP(CALL(VAR(\"slice\"),<args>**),<value>*,<op>*)");

	private CAstNode rewriteSubscriptAssign(Segments s) {
		int i = 0;
		CAstNode[] args = new CAstNode[ s.getMultiple("args").size() + 1];
		for(CAstNode arg : s.getMultiple("args")) {
			args[i++] = arg;
		}
		args[i++] = s.getSingle("value");
		
		return Ast.makeNode(CAstNode.CALL, Ast.makeNode(CAstNode.VAR, Ast.makeConstant("slice")), args);	
	}

	private CAstNode rewriteSubscriptAssignOp(Segments s) {
		int i = 0;
		CAstNode[] args = new CAstNode[ s.getMultiple("args").size() + 1];
		for(CAstNode arg : s.getMultiple("args")) {
			args[i++] = arg;
		}
		args[i++] = s.getSingle("value");
		
		return Ast.makeNode(CAstNode.CALL, Ast.makeNode(CAstNode.VAR, Ast.makeConstant("slice")), args);	
	}

	@Override
	protected TranslatorToCAst getTranslatorToCAst(CAst ast, ModuleEntry M) throws IOException {
		RewritingTranslatorToCAst x = new RewritingTranslatorToCAst(M, new PythonModuleParser((SourceModule)M, typeDictionary) {
			@Override
			public CAstEntity translateToCAst() throws Error, IOException {
				CAstEntity ce =  super.translateToCAst();
				return new AstConstantFolder().fold(ce);
			}
		});
		
		x.addRewriter(new CAstRewriterFactory<NonCopyingContext,NoKey>() {
			@Override
			public PatternBasedRewriter createCAstRewriter(CAst ast) {
				return new PatternBasedRewriter(ast, sliceAssign, (Segments s) -> { return rewriteSubscriptAssign(s); });
			}
		}, false);

		x.addRewriter(new CAstRewriterFactory<NonCopyingContext,NoKey>() {
			@Override
			public PatternBasedRewriter createCAstRewriter(CAst ast) {
				return new PatternBasedRewriter(ast, sliceAssignOp, (Segments s) -> { return rewriteSubscriptAssignOp(s); });
			}
		}, false);

		x.addRewriter(new CAstRewriterFactory<NonCopyingContext,NoKey>() {
			@Override
			public ConstantFoldingRewriter createCAstRewriter(CAst ast) {
				return new ConstantFoldingRewriter(ast) {
					@Override
					protected Object eval(CAstOperator op, Object lhs, Object rhs) {
						try {
							PyObject x = PythonUtil.getInterp().eval(lhs + " " + op.getValue() + " " + rhs);
							if (x.isNumberType()) {
								System.err.println(lhs + " " + op.getValue() + " " + rhs + " -> " + x.asInt());
								return x.asInt();
							}
						} catch (Exception e) {
							// interpreter died for some reason, so no information.
						}
						return null;
					}
				};
			}
			
		}, false);
		return x;
	}

	@Override
	protected boolean shouldTranslate(CAstEntity entity) {
		return true;
	}

	@Override
	protected TranslatorToIR initTranslator() {
		return new PythonCAstToIRTranslator(this);
	}

	final CoreClass Root = new CoreClass(PythonTypes.rootTypeName, null, this, null);

	final CoreClass Exception = new CoreClass(PythonTypes.Exception.getName(), PythonTypes.rootTypeName, this, null);

	final CoreClass CodeBody = new CoreClass(PythonTypes.CodeBody.getName(), PythonTypes.rootTypeName, this, null);

	final CoreClass lambda = new CoreClass(PythonTypes.lambda.getName(), PythonTypes.CodeBody.getName(), this, null);

	final CoreClass filter = new CoreClass(PythonTypes.filter.getName(), PythonTypes.CodeBody.getName(), this, null);

	final CoreClass comprehension = new CoreClass(PythonTypes.comprehension.getName(), PythonTypes.CodeBody.getName(), this, null);

	final CoreClass object = new CoreClass(PythonTypes.object.getName(), PythonTypes.rootTypeName, this, null);

	final CoreClass list = new CoreClass(PythonTypes.list.getName(), PythonTypes.object.getName(), this, null);

	final CoreClass set = new CoreClass(PythonTypes.set.getName(), PythonTypes.object.getName(), this, null);

	final CoreClass dict = new CoreClass(PythonTypes.dict.getName(), PythonTypes.object.getName(), this, null);

	final CoreClass tuple = new CoreClass(PythonTypes.tuple.getName(), PythonTypes.object.getName(), this, null);

	final CoreClass string = new CoreClass(PythonTypes.string.getName(), PythonTypes.object.getName(), this, null);

	final CoreClass trampoline = new CoreClass(PythonTypes.trampoline.getName(), PythonTypes.CodeBody.getName(), this, null);

	public class DynamicMethodBody extends DynamicCodeBody {
		private final IClass container;
		
		public DynamicMethodBody(TypeReference codeName, TypeReference parent, IClassLoader loader,
				Position sourcePosition, CAstEntity entity, WalkContext context, IClass container) {
			super(codeName, parent, loader, sourcePosition, entity, context);
			this.container = container;
		}

		public IClass getContainer() {
			return container;
		}
		
	}

	public IClass makeMethodBodyType(String name, TypeReference P, CAstSourcePositionMap.Position sourcePosition, CAstEntity entity, WalkContext context, IClass container) {
		return new DynamicMethodBody(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName(name)), P, this,
				sourcePosition, entity, context, container);
	}

	public IClass makeCodeBodyType(String name, TypeReference P, CAstSourcePositionMap.Position sourcePosition, CAstEntity entity, WalkContext context) {
		return new DynamicCodeBody(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName(name)), P, this,
				sourcePosition, entity, context);
	}

	public IClass defineFunctionType(String name, CAstSourcePositionMap.Position pos, CAstEntity entity, WalkContext context) {
		CAstType st = entity.getType().getSupertypes().iterator().next();
		return makeCodeBodyType(name, lookupClass(TypeName.findOrCreate("L" + st.getName())).getReference(), pos, entity, context);
	}

	public IClass defineMethodType(String name, CAstSourcePositionMap.Position pos, CAstEntity entity, TypeName typeName, WalkContext context) {
		PythonClass self = (PythonClass)types.get(typeName);

		IClass fun = makeMethodBodyType(name, PythonTypes.CodeBody, pos, entity, context, self);
		
		assert types.containsKey(typeName);
		MethodReference me = MethodReference.findOrCreate(fun.getReference(), Atom.findOrCreateUnicodeAtom(entity.getType().getName()), AstMethodReference.fnDesc);
		self.methodTypes.add(me);

		return fun;
	}

	public IMethod defineCodeBodyCode(String clsName, AbstractCFG<?, ?> cfg, SymbolTable symtab, boolean hasCatchBlock,
			Map<IBasicBlock<SSAInstruction>, TypeReference[]> caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo, int defaultArgs) {
		DynamicCodeBody C = (DynamicCodeBody) lookupClass(clsName, cha);
		assert C != null : clsName;
		return C.setCodeBody(makeCodeBodyCode(cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp, lexicalInfo, debugInfo, C, defaultArgs));
	}

	public DynamicMethodObject makeCodeBodyCode(AbstractCFG<?, ?> cfg, SymbolTable symtab, boolean hasCatchBlock,
			Map<IBasicBlock<SSAInstruction>, TypeReference[]> caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo,
			IClass C, int defaultArgs) {
		return new DynamicMethodObject(C, Collections.emptySet(), cfg, symtab, hasCatchBlock, caughtTypes, hasMonitorOp, lexicalInfo,
				debugInfo) {
					@Override
					public int getNumberOfDefaultParameters() {
						return defaultArgs;
					}			
		};
	}

	public class PythonClass extends CoreClass {
		private java.util.Set<IField> staticFields = HashSetFactory.make();
		private java.util.Set<MethodReference> methodTypes = HashSetFactory.make();
		private java.util.Set<TypeReference> innerTypes = HashSetFactory.make();
		
		public PythonClass(TypeName name, TypeName superName, IClassLoader loader, Position sourcePosition) {
			super(name, superName, loader, sourcePosition);
			if (name.toString().lastIndexOf('/') > 0) {
				String maybeOuterName = name.toString().substring(0, name.toString().lastIndexOf('/'));
				TypeName maybeOuter = TypeName.findOrCreate(maybeOuterName);
				if (types.containsKey(maybeOuter)) {
					IClass cls = types.get(maybeOuter);
					if (cls instanceof PythonClass) {
						((PythonClass)cls).innerTypes.add(this.getReference());
					}
				}
			}
		}

		@Override
		public Collection<IField> getDeclaredStaticFields() {
			return staticFields;
		}	
		
		public Collection<MethodReference> getMethodReferences() {
			return methodTypes;
		}

		public Collection<TypeReference> getInnerReferences() {
			return innerTypes;
		}
}
	
	public void defineType(TypeName cls, TypeName parent, Position sourcePosition) {
		new PythonClass(cls, parent, this, sourcePosition);
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
