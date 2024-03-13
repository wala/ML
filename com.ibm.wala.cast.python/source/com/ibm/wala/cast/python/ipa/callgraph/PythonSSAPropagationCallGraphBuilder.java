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

import com.ibm.wala.cast.ipa.callgraph.AstSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.ipa.callgraph.GlobalObjectKey;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.ssa.PythonInstructionVisitor;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.fixpoint.AbstractOperator;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.AbstractFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
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
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

public class PythonSSAPropagationCallGraphBuilder extends AstSSAPropagationCallGraphBuilder {

  @SuppressWarnings("unused")
  private static final Logger logger =
      Logger.getLogger(PythonSSAPropagationCallGraphBuilder.class.getName());

  public PythonSSAPropagationCallGraphBuilder(
      IClassHierarchy cha,
      AnalysisOptions options,
      IAnalysisCacheView cache,
      PointerKeyFactory pointerKeyFactory) {
    super(
        PythonLanguage.Python.getFakeRootMethod(cha, options, cache),
        options,
        cache,
        pointerKeyFactory);
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
    return definingMethod.equals(
        opNode.getMethod().getReference().getDeclaringClass().getName().toString());
  }

  private static final Collection<TypeReference> types =
      Arrays.asList(PythonTypes.string, TypeReference.Int);

  public static class PythonConstraintVisitor extends AstConstraintVisitor
      implements PythonInstructionVisitor {

    @Override
    protected PythonSSAPropagationCallGraphBuilder getBuilder() {
      return (PythonSSAPropagationCallGraphBuilder) builder;
    }

    public PythonConstraintVisitor(AstSSAPropagationCallGraphBuilder builder, CGNode node) {
      super(builder, node);
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
            Pair<String, TypeReference> key = Pair.make(name, ik.getConcreteType().getReference());
            // system.newConstraint(lvalKey, new ConcreteTypeKey(getBuilder().ensure(key)));
          }
        }
      } else {
        system.newSideEffect(
            new AbstractOperator<PointsToSetVariable>() {
              @Override
              public byte evaluate(PointsToSetVariable lhs, PointsToSetVariable[] rhs) {
                if (rhs[0].getValue() != null)
                  rhs[0]
                      .getValue()
                      .foreach(
                          (i) -> {
                            InstanceKey ik = system.getInstanceKey(i);
                            if (types.contains(ik.getConcreteType().getReference())) {
                              Pair<String, TypeReference> key =
                                  Pair.make(name, ik.getConcreteType().getReference());
                              // system.newConstraint(lvalKey, new
                              // ConcreteTypeKey(getBuilder().ensure(key)));
                            }
                          });
                return NOT_CHANGED;
              }

              @Override
              public int hashCode() {
                return node.hashCode() * instruction.hashCode();
              }

              @Override
              public boolean equals(Object o) {
                return getClass().equals(o.getClass()) && hashCode() == o.hashCode();
              }

              @Override
              public String toString() {
                return "get function " + name + " at " + instruction;
              }
            },
            new PointerKey[] {lvalKey});
      }

      // TODO Auto-generated method stub
      super.visitGet(instruction);
    }

    @Override
    public void visitPythonInvoke(PythonInvokeInstruction inst) {
      visitInvokeInternal(inst, new DefaultInvariantComputer());
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
  protected void processCallingConstraints(
      CGNode caller,
      SSAAbstractInvokeInstruction instruction,
      CGNode target,
      InstanceKey[][] constParams,
      PointerKey uniqueCatchKey) {

    if (!(instruction instanceof PythonInvokeInstruction)) {
      super.processCallingConstraints(caller, instruction, target, constParams, uniqueCatchKey);
    } else {
      MutableIntSet args = IntSetUtil.make();

      // positional parameters
      PythonInvokeInstruction call = (PythonInvokeInstruction) instruction;
      for (int i = 0;
          i < call.getNumberOfPositionalParameters()
              && i < target.getMethod().getNumberOfParameters();
          i++) {
        PointerKey lval = getPointerKeyForLocal(target, i + 1);
        args.add(i);

        if (constParams != null && constParams[i] != null) {
          InstanceKey[] ik = constParams[i];
          for (InstanceKey element : ik) {
            system.newConstraint(lval, element);
          }
        } else {
          PointerKey rval = getPointerKeyForLocal(caller, call.getUse(i));

          // If we are looking at the implicit parameter of a callable.
          if (call.getCallSite().isDispatch() && i == 0 && refersToAnObject(rval)) {
            // Ensure that lval's variable refers to the callable method instead of callable object.
            IClass callable = target.getMethod().getDeclaringClass();
            IntSet instanceKeysForCallable = this.getSystem().getInstanceKeysForClass(callable);

            for (IntIterator it = instanceKeysForCallable.intIterator(); it.hasNext(); ) {
              int instanceKeyIndex = it.next();
              InstanceKey instanceKey = this.getSystem().getInstanceKey(instanceKeyIndex);
              this.getSystem().newConstraint(lval, instanceKey);
            }
          } else {
            getSystem().newConstraint(lval, assignOperator, rval);
          }
        }
      }

      // keyword arguments
      int paramNumber = call.getNumberOfPositionalParameters();
      keywords:
      for (String argName : call.getKeywords()) {
        int src = call.getUse(argName);
        for (int i = 0; i < target.getIR().getSymbolTable().getMaxValueNumber(); i++) {
          String[] paramNames = target.getIR().getLocalNames(0, i + 1);
          if (paramNames != null) {
            for (String destName : paramNames) {
              if (argName.equals(destName)) {
                PointerKey lval = getPointerKeyForLocal(target, i + 1);
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

      int dflts =
          target.getMethod().getNumberOfParameters()
              - target.getMethod().getNumberOfDefaultParameters();
      for (int i = dflts; i < target.getMethod().getNumberOfParameters(); i++) {
        if (!args.contains(i)) {
          String name = target.getMethod().getDeclaringClass().getName() + "_defaults_" + i;
          FieldReference global =
              FieldReference.findOrCreate(
                  PythonTypes.Root,
                  Atom.findOrCreateUnicodeAtom("global " + name),
                  PythonTypes.Root);
          IField f = getClassHierarchy().resolveField(global);
          PointerKey lval = getPointerKeyForLocal(target, i + 1);
          getSystem().newConstraint(lval, assignOperator, new StaticFieldKey(f));
        }
      }

      // return values
      PointerKey rret = getPointerKeyForReturnValue(target);
      PointerKey lret = getPointerKeyForLocal(caller, call.getReturnValue(0));
      getSystem().newConstraint(lret, assignOperator, rret);
    }
  }

  /**
   * Returns true iff the given {@link PointerKey} points to at least one instance whose concrete
   * type equals {@link PythonTypes#object}.
   *
   * @param pointerKey The {@link PointerKey} in question.
   * @return True iff the given {@link PointerKey} points to at least one object whose concrete type
   *     equals {@link PythonTypes#object}.,
   */
  protected boolean refersToAnObject(PointerKey pointerKey) {
    PointerAnalysis<InstanceKey> pointerAnalysis = this.getPointerAnalysis();
    OrdinalSet<InstanceKey> pointsToSet = pointerAnalysis.getPointsToSet(pointerKey);

    for (InstanceKey instanceKey : pointsToSet) {
      IClass concreteType = instanceKey.getConcreteType();
      TypeReference reference = concreteType.getReference();

      // If it's an "object" method.
      if (reference.equals(PythonTypes.object)) return true;
    }

    return false;
  }

  @Override
  public PythonConstraintVisitor makeVisitor(CGNode node) {
    return new PythonConstraintVisitor(this, node);
  }

  public static class PythonInterestingVisitor extends AstInterestingVisitor
      implements PythonInstructionVisitor {
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
