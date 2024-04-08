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

import com.google.common.collect.Maps;
import com.ibm.wala.cast.ipa.callgraph.AstSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.ipa.callgraph.GlobalObjectKey;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstPropertyRead;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.ssa.PythonInstructionVisitor;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.fixpoint.AbstractOperator;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
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
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    private static final String GLOBAL_IDENTIFIER = "global";

    private static final String IMPORT_WILDCARD_CHARACTER = "*";

    private static final Atom IMPORT_FUNCTION_NAME = Atom.findOrCreateAsciiAtom("import");

    /**
     * A mapping of script names to wildcard imports. We use a {@link Deque} here because we want to
     * always examine the last (front of the queue) encountered wildcard import library for known
     * names assuming that import instructions are traversed from first to last.
     */
    private static Map<String, Deque<MethodReference>> scriptToWildcardImports = Maps.newHashMap();

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

    @Override
    public void visitPropertyRead(AstPropertyRead instruction) {
      super.visitPropertyRead(instruction);

      int memberRef = instruction.getMemberRef();

      if (this.ir.getSymbolTable().isConstant(memberRef)) {
        Object constantValue = this.ir.getSymbolTable().getConstantValue(memberRef);

        if (Objects.equals(constantValue, IMPORT_WILDCARD_CHARACTER)) {
          // We have a wildcard.
          logger.fine("Detected wildcard for " + memberRef + " in " + instruction + ".");

          int objRef = instruction.getObjectRef();
          logger.fine("Seeing if " + objRef + " refers to an import.");

          SSAInstruction def = this.du.getDef(objRef);
          logger.finer("Found definition: " + def + ".");

          TypeName scriptTypeName =
              this.ir.getMethod().getReference().getDeclaringClass().getName();
          assert scriptTypeName.getPackage() == null
              : "Import statement should only occur at the top-level script.";

          String scriptName = scriptTypeName.getClassName().toString();

          if (def instanceof SSAInvokeInstruction) {
            // Library case.
            SSAInvokeInstruction invokeInstruction = (SSAInvokeInstruction) def;
            MethodReference declaredTarget = invokeInstruction.getDeclaredTarget();
            Atom declaredTargetName = declaredTarget.getName();

            if (declaredTargetName.equals(IMPORT_FUNCTION_NAME)) {
              // It's an import "statement" importing a library.
              logger.fine("Found library import statement in: " + scriptTypeName + ".");

              logger.info(
                  "Adding: "
                      + declaredTarget.getDeclaringClass().getName().getClassName()
                      + " to wildcard imports for: "
                      + scriptName
                      + ".");

              // Add the library to the script's queue of wildcard imports.
              scriptToWildcardImports.compute(
                  scriptName,
                  (k, v) -> {
                    if (v == null) {
                      Deque<MethodReference> deque = new ArrayDeque<>();
                      deque.push(declaredTarget);
                      return deque;
                    } else {
                      v.push(declaredTarget);
                      return v;
                    }
                  });
            }
          } else if (def instanceof SSAGetInstruction) {
            // We are importing from a script.
            SSAGetInstruction getInstruction = (SSAGetInstruction) def;
            FieldReference declaredField = getInstruction.getDeclaredField();
            Atom fieldName = declaredField.getName();
            String strippedFieldName =
                fieldName.toString().substring(GLOBAL_IDENTIFIER.length() + 1);
            TypeReference typeReference =
                TypeReference.findOrCreate(PythonTypes.pythonLoader, "L" + strippedFieldName);
            MethodReference methodReference =
                MethodReference.findOrCreate(
                    typeReference,
                    Atom.findOrCreateAsciiAtom("do"),
                    Descriptor.findOrCreate(null, PythonTypes.rootTypeName));

            logger.info(
                "Adding: "
                    + methodReference.getDeclaringClass().getName().getClassName()
                    + " to wildcard imports for: "
                    + scriptName
                    + ".");

            // Add the script to the queue of this script's wildcard imports.
            scriptToWildcardImports.compute(
                scriptName,
                (k, v) -> {
                  if (v == null) {
                    Deque<MethodReference> deque = new ArrayDeque<>();
                    deque.push(methodReference);
                    return deque;
                  } else {
                    v.push(methodReference);
                    return v;
                  }
                });
          }
        }
      }
    }

    @Override
    public void visitAstGlobalRead(AstGlobalRead globalRead) {
      super.visitAstGlobalRead(globalRead);

      TypeName scriptTypeName = this.ir.getMethod().getReference().getDeclaringClass().getName();

      String scriptName =
          (scriptTypeName.getPackage() == null
                  ? scriptTypeName.getClassName()
                  : scriptTypeName.getPackage())
              .toString();
      logger.finer("Script name is: " + scriptName + ".");

      PointerKey globalDefPK = this.getPointerKeyForLocal(globalRead.getDef());
      assert globalDefPK != null;

      // Are there any wildcard imports for this script?
      if (scriptToWildcardImports.containsKey(scriptName)) {
        logger.info("Found wildcard imports in " + scriptName + " for " + globalRead + ".");

        Deque<MethodReference> deque = scriptToWildcardImports.get(scriptName);

        for (MethodReference importMethodReference : deque) {
          logger.fine(
              "Library with wildcard import is: "
                  + importMethodReference.getDeclaringClass().getName().getClassName()
                  + ".");

          String globalFieldName = getStrippedDeclaredFieldName(globalRead);
          logger.fine("Examining global: " + globalFieldName + " for wildcard import.");

          CallGraph callGraph = this.getBuilder().getCallGraph();
          Set<CGNode> nodes = callGraph.getNodes(importMethodReference);

          for (CGNode n : nodes) {
            for (Iterator<NewSiteReference> nit = n.iterateNewSites(); nit.hasNext(); ) {
              NewSiteReference newSiteReference = nit.next();

              String name = newSiteReference.getDeclaredType().getName().getClassName().toString();
              logger.finest("Examining: " + name + ".");

              if (name.equals(globalFieldName)) {
                logger.info("Found wildcard import for: " + name + ".");

                InstanceKey instanceKey = this.getInstanceKeyForAllocation(newSiteReference);

                if (this.system.newConstraint(globalDefPK, instanceKey)) {
                  logger.fine(
                      "Added constraint that: " + globalDefPK + " gets: " + instanceKey + ".");
                  return;
                }
              }
            }

            // Also check the put instructions, as these may be generated by the initialization
            // file.
            n.getIR()
                .visitNormalInstructions(
                    new PythonInstructionVisitor() {

                      @Override
                      public void visitPut(SSAPutInstruction putInstruction) {
                        FieldReference putField = putInstruction.getDeclaredField();

                        if (globalFieldName.equals(putField.getName().toString())) {
                          // Found it.
                          int putVal = putInstruction.getVal();

                          // Make the global def point to the put instruction value.
                          PointerKey putValPK =
                              PythonSSAPropagationCallGraphBuilder.PythonConstraintVisitor.this
                                  .getBuilder()
                                  .getPointerKeyForLocal(n, putVal);

                          if (PythonSSAPropagationCallGraphBuilder.PythonConstraintVisitor.this
                              .system.newConstraint(globalDefPK, assignOperator, putValPK))
                            logger.fine(
                                "Added constraint that: "
                                    + globalDefPK
                                    + " gets: "
                                    + putValPK
                                    + ".");
                          return;
                        }
                      }
                    });
          }
        }
      }
    }

    private static String getStrippedDeclaredFieldName(SSAGetInstruction instruction) {
      String declaredFieldName = instruction.getDeclaredField().getName().toString();
      assert declaredFieldName.startsWith(GLOBAL_IDENTIFIER + " ");

      // Remove the global identifier.
      String strippedDeclaredFieldName =
          declaredFieldName.substring(
              (GLOBAL_IDENTIFIER + " ").length(), declaredFieldName.length());
      return strippedDeclaredFieldName;
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
