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
package com.ibm.wala.cast.python.ipa.callgraph;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.ibm.wala.cast.ipa.callgraph.AstSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.ipa.callgraph.GlobalObjectKey;
import com.ibm.wala.cast.python.ipa.summaries.BuiltinFunctions.BuiltinFunction;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.ssa.PythonInstructionVisitor;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.ssa.PythonStoreProperty;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.fixpoint.AbstractOperator;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.AbstractFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.strings.Atom;

public class PythonSSAPropagationCallGraphBuilder extends AstSSAPropagationCallGraphBuilder {

	public PythonSSAPropagationCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache,
			PointerKeyFactory pointerKeyFactory) {
		super(PythonLanguage.Python.getFakeRootMethod(cha, options, cache), options, cache, pointerKeyFactory);
	}

	protected boolean isConstantRef(SymbolTable symbolTable, int valueNumber) {
		return valueNumber != -1 && symbolTable.isConstant(valueNumber);
	}

	@Override
	protected boolean useObjectCatalog() {
		return true;
	}

	@Override
	public GlobalObjectKey getGlobalObject(Atom language) {
		assert language.equals(PythonLanguage.Python.getName());
		return new GlobalObjectKey(cha.lookupClass(PythonTypes.Root));
	}

	@Override
	protected AbstractFieldPointerKey fieldKeyForUnknownWrites(AbstractFieldPointerKey fieldKey) {
		return null;
	}

	@Override
	protected boolean sameMethod(CGNode opNode, String definingMethod) {
	    return definingMethod.equals(opNode.getMethod().getReference().getDeclaringClass().getName().toString());
	}

	private static final Collection<TypeReference> types = Arrays.asList(PythonTypes.string, TypeReference.Int);

	public static class PythonConstraintVisitor extends AstConstraintVisitor implements PythonInstructionVisitor {

		public PythonConstraintVisitor(AstSSAPropagationCallGraphBuilder builder, CGNode node) {
			super(builder, node);
		}

		private final Map<Pair<String,TypeReference>,BuiltinFunction> primitives = HashMapFactory.make();
		
		private BuiltinFunction ensure(Pair<String,TypeReference> key) {
			if (! primitives.containsKey(key)) {
				primitives.put(key, new BuiltinFunction(this.getClassHierarchy(), key.fst, key.snd));
			}
			
			return primitives.get(key);
		}
		
		@Override
		public void visitGet(SSAGetInstruction instruction) {
		      SymbolTable symtab = ir.getSymbolTable();
    		  String name = instruction.getDeclaredField().getName().toString();

		      int objVn = instruction.getRef();
		      final PointerKey objKey = getPointerKeyForLocal(objVn);

		      int lvalVn = instruction.getDef();
		      final PointerKey lvalKey = getPointerKeyForLocal(lvalVn);

		      if (contentsAreInvariant(symtab, du, objVn)) {
		          system.recordImplicitPointsToSet(objKey);
		          for (InstanceKey ik : getInvariantContents(objVn)) {
		        	  if (types.contains(ik.getConcreteType().getReference())) {
		        		  Pair<String,TypeReference> key = Pair.make(name, ik.getConcreteType().getReference());
		        		  system.newConstraint(lvalKey, new ConcreteTypeKey(ensure(key)));
		        	  }
		          }
		      } else {
		    	  system.newSideEffect(new AbstractOperator<PointsToSetVariable>() {
					@Override
					public byte evaluate(PointsToSetVariable lhs, PointsToSetVariable[] rhs) {
						if (rhs[0].getValue() != null)
						rhs[0].getValue().foreach((i) -> { 
							InstanceKey ik = system.getInstanceKey(i);
							if (types.contains(ik.getConcreteType().getReference())) {
								Pair<String,TypeReference> key = Pair.make(name, ik.getConcreteType().getReference());
								system.newConstraint(lvalKey, new ConcreteTypeKey(ensure(key)));
							}
						});
						return NOT_CHANGED;
					}

					@Override
					public int hashCode() {
						return node.hashCode()*instruction.hashCode();
					}

					@Override
					public boolean equals(Object o) {
						return getClass().equals(o.getClass()) && hashCode() == o.hashCode();
					}

					@Override
					public String toString() {
						return "get function " + name + " at " + instruction;
					}  
		    	  }, new PointerKey[] { lvalKey });
		      }
		      
			// TODO Auto-generated method stub
			super.visitGet(instruction);
		}


		@Override
		public void visitPythonInvoke(PythonInvokeInstruction inst) {
	        visitInvokeInternal(inst, new DefaultInvariantComputer());
		}

		@Override
		public void visitPythonStoreProperty(PythonStoreProperty inst) {
			newFieldWrite(node, inst.getArrayRef(), inst.getIndex(), inst.getValue());
		}

		@Override
		public void visitArrayLoad(SSAArrayLoadInstruction inst) {
			newFieldRead(node, inst.getArrayRef(), inst.getIndex(), inst.getDef());			
		}

		@Override
		public void visitArrayStore(SSAArrayStoreInstruction inst) {
			newFieldWrite(node, inst.getArrayRef(), inst.getIndex(), inst.getValue());
		}
		
		
	}

	@Override
	protected void processCallingConstraints(CGNode caller, SSAAbstractInvokeInstruction instruction, CGNode target,
			InstanceKey[][] constParams, PointerKey uniqueCatchKey) {
		
		if (target.toString().contains("numpy_input_fn")) {
			System.err.println(target);
		}
		
		if (! (instruction instanceof PythonInvokeInstruction)) {
			super.processCallingConstraints(caller, instruction, target, constParams, uniqueCatchKey);
		} else {
			MutableIntSet args = IntSetUtil.make();
			
			// positional parameters
			PythonInvokeInstruction call = (PythonInvokeInstruction) instruction;
			for(int i = 0; i < call.getNumberOfPositionalParameters(); i++) {
				PointerKey lval = getPointerKeyForLocal(target, i+1);
				args.add(i);
				
				if (constParams != null && constParams[i] != null) {
					InstanceKey[] ik = constParams[i];
					for (InstanceKey element : ik) {
						system.newConstraint(lval, element);
					}		
				} else {
					PointerKey rval = getPointerKeyForLocal(caller, call.getUse(i));
					getSystem().newConstraint(lval, assignOperator, rval);
				}
			}
			
			// keyword arguments
			int paramNumber = call.getNumberOfPositionalParameters();
			keywords: for(String argName : call.getKeywords()) {
				int src = call.getUse(argName);
				for(int i = 0; i < target.getIR().getSymbolTable().getMaxValueNumber(); i++) {
					String[] paramNames = target.getIR().getLocalNames(0, i+1);
					if (paramNames != null) {
						for(String destName : paramNames) {
							if (argName.equals(destName)) {
								PointerKey lval = getPointerKeyForLocal(target, i+1);
								args.add(i);
								int p = paramNumber;
								if (constParams != null && constParams[p] != null) {
									InstanceKey[] ik = constParams[p];
									for (InstanceKey element : ik) {
										system.newConstraint(lval, element);
									}		
								} else {
									PointerKey rval = getPointerKeyForLocal(caller, src);
									getSystem().newConstraint(lval, assignOperator, rval);
								}
								paramNumber++;
								continue keywords;
							}
						}
					}	
				}
				// no such argument in callee
				paramNumber++;					
			}

			int dflts = target.getMethod().getNumberOfParameters() - target.getMethod().getNumberOfDefaultParameters();
			for(int i = dflts; i < target.getMethod().getNumberOfParameters(); i++) {
				if (! args.contains(i)) {
					String name = target.getMethod().getDeclaringClass().getName() + "_defaults_" + i;
					FieldReference global = FieldReference.findOrCreate(PythonTypes.Root, Atom.findOrCreateUnicodeAtom("global " + name), PythonTypes.Root);
					IField f = getClassHierarchy().resolveField(global);
					PointerKey lval = getPointerKeyForLocal(target, i+1);
					getSystem().newConstraint(lval, assignOperator, new StaticFieldKey(f));
				}
			}

			// return values
			PointerKey rret = getPointerKeyForReturnValue(target);
			PointerKey lret = getPointerKeyForLocal(caller, call.getReturnValue(0));
			getSystem().newConstraint(lret, assignOperator, rret);
		}
	}

	@Override
	public PythonConstraintVisitor makeVisitor(CGNode node) {
		return new PythonConstraintVisitor(this, node);
	}

	public static class PythonInterestingVisitor extends AstInterestingVisitor implements PythonInstructionVisitor {
		public PythonInterestingVisitor(int vn) {
			super(vn);
		}

		@Override
		public void visitBinaryOp(final SSABinaryOpInstruction instruction) {
			bingo = true;
		}

		@Override
		public void visitPythonInvoke(PythonInvokeInstruction inst) {
			bingo = true;
		}
	}

	@Override
	protected InterestingVisitor makeInterestingVisitor(CGNode node, int vn) {
		return new PythonInterestingVisitor(vn);
	}

}
