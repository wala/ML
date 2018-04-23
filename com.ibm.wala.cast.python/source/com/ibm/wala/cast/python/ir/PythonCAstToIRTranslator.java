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
package com.ibm.wala.cast.python.ir;

import static com.ibm.wala.cast.python.ir.PythonLanguage.Python;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.ibm.wala.cast.ir.translator.ArrayOpHandler;
import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.cast.loader.DynamicCallSiteReference;
import com.ibm.wala.cast.python.loader.PythonLoader;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import com.ibm.wala.cast.tree.visit.CAstVisitor;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction.IOperator;
import com.ibm.wala.shrikeBT.IInvokeInstruction.Dispatch;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.strings.Atom;

public class PythonCAstToIRTranslator extends AstTranslator {

	public PythonCAstToIRTranslator(IClassLoader loader, Map<Object, CAstEntity> namedEntityResolver,
			ArrayOpHandler arrayOpHandler) {
		super(loader, namedEntityResolver, arrayOpHandler);
	}

	public PythonCAstToIRTranslator(IClassLoader loader, Map<Object, CAstEntity> namedEntityResolver) {
		super(loader, namedEntityResolver);
	}

	public PythonCAstToIRTranslator(IClassLoader loader) {
		super(loader);
	}

	@Override
	protected boolean hasImplicitGlobals() {
		return true;
	}

	@Override
	protected boolean useDefaultInitValues() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean treatGlobalsAsLexicallyScoped() {
		return false;
	}

	@Override
	protected TypeReference defaultCatchType() {
		return PythonTypes.Exception;
	}

	@Override
	protected TypeReference makeType(CAstType type) {
		return TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName(type.getName()));
	}

	@Override
	protected boolean defineType(CAstEntity type, WalkContext wc) {
		CAstType cls = type.getType();
		wc.getGlobalScope().declare(new CAstSymbolImpl(cls.getName(), cls));
			
		((PythonLoader)loader)
		    .defineType(
		    		TypeName.findOrCreate("L" + cls.getName()), 
		    		cls.getSupertypes().isEmpty()?
		    			PythonTypes.object.getName():
		    			TypeName.findOrCreate("L" + cls.getSupertypes().iterator().next().getName()));
		
		return true;
	}

	@Override
	protected IOperator translateBinaryOpcode(CAstNode op) {
		if (CAstOperator.OP_IN == op || CAstOperator.OP_POW == op) {
			return IBinaryOpInstruction.Operator.ADD;
		} else {
			return super.translateBinaryOpcode(op);
		}
	}

	@Override
	protected void declareFunction(CAstEntity N, WalkContext context) {
		for (String s : N.getArgumentNames()) {
			context.currentScope().declare(new CAstSymbolImpl(s, Any));
		}
		
	    String fnName = composeEntityName(context, N);
	    if (N.getType() instanceof CAstType.Method) {
	    		((PythonLoader) loader).defineMethodType("L" + fnName, N.getPosition(), N, ((CAstType.Method)N.getType()).getDeclaringType(), context);	    	
	    } else {
	    		((PythonLoader) loader).defineFunctionType("L" + fnName, N.getPosition(), N, context);
	    }
	}

	@Override
	protected void defineFunction(CAstEntity N, WalkContext definingContext,
			AbstractCFG<SSAInstruction, ? extends IBasicBlock<SSAInstruction>> cfg, SymbolTable symtab,
			boolean hasCatchBlock, Map<IBasicBlock<SSAInstruction>, TypeReference[]> catchTypes, boolean hasMonitorOp,
			AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
	      String fnName = composeEntityName(definingContext, N);

	      ((PythonLoader) loader).defineCodeBodyCode("L" + fnName, cfg, symtab, hasCatchBlock, catchTypes, hasMonitorOp, lexicalInfo,
	          debugInfo);
	}

	@Override
	protected void defineField(CAstEntity topEntity, WalkContext context, CAstEntity fieldEntity) {
		((PythonLoader)loader).defineField(TypeName.findOrCreate("L" + topEntity.getType().getName()), fieldEntity);
	}

	@Override
	protected String composeEntityName(WalkContext parent, CAstEntity f) {
		if (f.getType() instanceof CAstType.Method) {
			return ((CAstType.Method)f.getType()).getDeclaringType().getName() + "/" + f.getName();
		} else {
			return f.getName();
		}
	}

	@Override
	protected void doThrow(WalkContext context, int exception) {
		context.cfg().addInstruction(Python.instructionFactory().ThrowInstruction(context.cfg().getCurrentInstruction(), exception));
	}

	@Override
	public void doArrayRead(WalkContext context, int result, int arrayValue, CAstNode arrayRef, int[] dimValues) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doArrayWrite(WalkContext context, int arrayValue, CAstNode arrayRef, int[] dimValues, int rval) {
		assert dimValues.length == 1;
		context.cfg().addInstruction(Python.instructionFactory().ArrayStoreInstruction(context.cfg().getCurrentInstruction(), arrayValue, dimValues[0], rval, PythonTypes.Root));
	}

	@Override
	protected void doFieldRead(WalkContext context, int result, int receiver, CAstNode elt, CAstNode parent) {
	    if (elt.getKind() == CAstNode.CONSTANT && elt.getValue() instanceof String) {
			FieldReference f = FieldReference.findOrCreate(PythonTypes.Root, Atom.findOrCreateUnicodeAtom((String)elt.getValue()), PythonTypes.Root);
			context.cfg().addInstruction(Python.instructionFactory().GetInstruction(context.cfg().getCurrentInstruction(), result, receiver, f));
		} else {

		}
	}

	@Override
	protected void doFieldWrite(WalkContext context, int receiver, CAstNode elt, CAstNode parent, int rval) {
	    if (elt.getKind() == CAstNode.CONSTANT && elt.getValue() instanceof String) {
			FieldReference f = FieldReference.findOrCreate(PythonTypes.Root, Atom.findOrCreateUnicodeAtom((String)elt.getValue()), PythonTypes.Root);
			context.cfg().addInstruction(Python.instructionFactory().PutInstruction(context.cfg().getCurrentInstruction(), receiver, rval, f));
		} else {

		}
	}

	@Override
	protected void doMaterializeFunction(CAstNode node, WalkContext context, int result, int exception, CAstEntity fn) {
	    String fnName = composeEntityName(context, fn);
	    IClass cls = loader.lookupClass(TypeName.findOrCreate("L" + fnName));
	    TypeReference type = cls.getReference();
	    int idx = context.cfg().getCurrentInstruction();
	    context.cfg().addInstruction(Python.instructionFactory().NewInstruction(idx, result, NewSiteReference.make(idx, type)));
	}

	@Override
	protected void leaveTypeEntity(CAstEntity n, WalkContext context, WalkContext typeContext, CAstVisitor<WalkContext> visitor) {
		super.leaveTypeEntity(n, context, typeContext, visitor);
		
		int v = context.currentScope().allocateTempValue();
		
		int idx = context.cfg().getCurrentInstruction();
	    String fnName = composeEntityName(context, n);
	    IClass cls = loader.lookupClass(TypeName.findOrCreate("L" + fnName));
	    TypeReference type = cls.getReference();
	    context.cfg().addInstruction(Python.instructionFactory().NewInstruction(idx, v, NewSiteReference.make(idx, type)));
	
	    doGlobalWrite(context, fnName, type, v);

	    for(CAstEntity field : n.getAllScopedEntities().get(null)) {
   			FieldReference fr = FieldReference.findOrCreate(type, Atom.findOrCreateUnicodeAtom(field.getName()), PythonTypes.Root);
   			int val;
	    		if (field.getKind() == CAstEntity.FIELD_ENTITY) {
	    			this.visit(field.getAST(), context, this);
	    			val = context.getValue(field.getAST());
	    		} else {
	    			assert (field.getKind() == CAstEntity.FUNCTION_ENTITY);
	    			val = context.currentScope().allocateTempValue();
	    			doMaterializeFunction(null, context, val, -1, field);	    			
	    		}
	    		context.cfg().addInstruction(Python.instructionFactory().PutInstruction(context.cfg().getCurrentInstruction(), v, val, fr));
	    }
	}

	@Override
	protected void doNewObject(WalkContext context, CAstNode newNode, int result, Object type, int[] arguments) {
		context.cfg().addInstruction(
			insts.NewInstruction(context.cfg().getCurrentInstruction(), 
				result, 
				NewSiteReference.make(
						context.cfg().getCurrentInstruction(), 
						TypeReference.findOrCreate(
								PythonTypes.pythonLoader, 
								"L" + type))));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doCall(WalkContext context, CAstNode call, int result, int exception, CAstNode name, int receiver,
			int[] arguments) {
		int pos = context.cfg().getCurrentInstruction();
		CallSiteReference site = new DynamicCallSiteReference(PythonTypes.CodeBody, pos);
		
		List<Integer> posp = new ArrayList<Integer>();
		List<Pair<String,Integer>> keyp = new ArrayList<Pair<String,Integer>>();
		posp.add(receiver);
		for(int i = 2; i < call.getChildCount(); i++) {
			CAstNode cl = call.getChild(i);
			if (cl.getKind() == CAstNode.ARRAY_LITERAL) {
				keyp.add(Pair.make(String.valueOf(cl.getChild(0).getValue()), context.getValue(cl.getChild(1))));
			} else {
				posp.add(context.getValue(cl));
			}
		}
		
		int params[] = new int[ arguments.length+1 ];
		params[0] = receiver;
		System.arraycopy(arguments, 0, params, 1, arguments.length);
		
		int[] hack = new int[ posp.size() ];
		for(int i = 0; i < hack.length; i++) {
			hack[i] = posp.get(i);
		}
		
		context.cfg().addInstruction(new PythonInvokeInstruction(pos, result, exception, site, hack, keyp.toArray(new Pair[ keyp.size() ])));
	}

	  public static final CAstType Any = new CAstType() {

		    @Override
		    public String getName() {
		      return "Any";
		    }

		    @Override
		    public Collection<CAstType> getSupertypes() {
		      return Collections.emptySet();
		    }
		  };
		  
		  @Override
		  protected CAstType topType() {
		    return Any;
		  }

		  public final CAstType Exception = new CAstType() {

			    @Override
			    public String getName() {
			      return "Exception";
			    }

			    @Override
			    public Collection<CAstType> getSupertypes() {
			      return Collections.singleton(topType());
			    }
			  };

		  @Override
		  protected CAstType exceptionType() {
		    return Any;
		  }

	@Override
	protected void doPrimitive(int resultVal, WalkContext context, CAstNode primitiveCall) {
		if (primitiveCall.getChildCount() == 2 && "import".equals(primitiveCall.getChild(0).getValue())) {
			TypeReference imprt = TypeReference.findOrCreate(PythonTypes.pythonLoader, "L" + primitiveCall.getChild(1).getValue());
			MethodReference call = MethodReference.findOrCreate(imprt, "import", "()L" + primitiveCall.getChild(1).getValue());
			int idx = context.cfg().getCurrentInstruction();
			context.cfg().addInstruction(Python.instructionFactory().InvokeInstruction(idx, resultVal, new int[0], context.currentScope().allocateTempValue(), CallSiteReference.make(idx, call, Dispatch.STATIC), null));
		}
	}
	
	@Override
	protected boolean visitVarAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c,
			CAstVisitor<WalkContext> visitor) {
		String name = n.getChild(0).getValue().toString();
		if (! c.currentScope().contains(name)) {
			c .currentScope().declare(new CAstSymbolImpl(name, PythonCAstToIRTranslator.Any));
		}
		
		return false;
	}

	@Override
	protected boolean visitVarAssignOp(CAstNode n, CAstNode v, CAstNode a, boolean pre, WalkContext c,
			CAstVisitor<WalkContext> visitor) {
		return visitVarAssign(n, v, a, c, visitor);
	}


	@Override
	protected void leaveArrayLiteralAssign(CAstNode n, CAstNode v, CAstNode a, WalkContext c,
			CAstVisitor<WalkContext> visitor) {
		int rval = c.getValue(v);
		for(int i = 0; i < n.getChildCount(); i++) {
			CAstNode var = n.getChild(i);
			String name = (String) var.getChild(0).getValue();
			c.currentScope().declare(new CAstSymbolImpl(name, topType()));
		    Symbol ls = c.currentScope().lookup(name);
		    
		    int rvi = c.currentScope().allocateTempValue();
		    int idx = c.currentScope().getConstantValue(i);
		    c.cfg().addInstruction(Python.instructionFactory().ArrayLoadInstruction(c.cfg().getCurrentInstruction(), rvi, rval, idx, PythonTypes.Root));
		    
		    c.setValue(n, rvi);
		    assignValue(n, c, ls, name, rvi);
		}
	}

	@Override
	protected boolean doVisit(CAstNode n, WalkContext context, CAstVisitor<WalkContext> visitor) {
		if (n.getKind() == CAstNode.COMPREHENSION_EXPR) {
			context.setValue(n, context.currentScope().getConstantValue(null));
			return true;
		} else {
			return super.doVisit(n, context, visitor);
		}
	}

}
