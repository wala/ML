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

import static com.ibm.wala.cast.python.util.Util.IMPORT_WILDCARD_CHARACTER;
import static com.ibm.wala.cast.python.util.Util.MODULE_INITIALIZATION_FILENAME;
import static com.ibm.wala.cast.python.util.Util.PYTHON_FILE_EXTENSION;

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
import com.ibm.wala.core.util.CancelRuntimeException;
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
import com.ibm.wala.util.CancelException;
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

  /**
   * A mapping of script names to wildcard imports. We use a {@link Deque} here because we want to
   * always examine the last (front of the queue) encountered wildcard import library for known
   * names assuming that import instructions are traversed from first to last.
   */
  private Map<String, Deque<MethodReference>> scriptToWildcardImports = Maps.newHashMap();

  public static class PythonConstraintVisitor extends AstConstraintVisitor
      implements PythonInstructionVisitor {

    private static final String GLOBAL_IDENTIFIER = "global";

    private static final Atom IMPORT_FUNCTION_NAME = Atom.findOrCreateAsciiAtom("import");

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

      if (this.ir.getSymbolTable().isConstant(instruction.getMemberRef())) {
        Object constantValue =
            this.ir.getSymbolTable().getConstantValue(instruction.getMemberRef());

        if (Objects.equals(constantValue, IMPORT_WILDCARD_CHARACTER)) {
          // We have a wildcard.
          logger.fine(
              "Detected wildcard for " + instruction.getMemberRef() + " in " + instruction + ".");

          processWildcardImports(instruction);
        }

        // check if we are reading from an module initialization script.
        SSAInstruction objRefDef = du.getDef(instruction.getObjectRef());
        logger.finest(
            () ->
                "Found def: "
                    + objRefDef
                    + " for object reference: "
                    + instruction.getObjectRef()
                    + " in instruction: "
                    + instruction
                    + ".");

        if (objRefDef instanceof AstGlobalRead) {
          AstGlobalRead agr = (AstGlobalRead) objRefDef;
          String fieldName = getStrippedDeclaredFieldName(agr);
          logger.finer("Found field name: " + fieldName);

          // if the "receiver" is a module initialization script.
          if (fieldName.toString().endsWith("/" + MODULE_INITIALIZATION_FILENAME))
            try {
              processWildcardImports(instruction, fieldName, constantValue.toString());
            } catch (CancelException e) {
              throw new CancelRuntimeException(e);
            }
        }
      }
    }

    /**
     * Processes the given {@link AstPropertyRead} for any potential wildcard imports being utilized
     * by the instruction.
     *
     * @param instruction The {@link AstPropertyRead} whose definition may depend on a wildcard
     *     import.
     */
    private void processWildcardImports(AstPropertyRead instruction) {
      int objRef = instruction.getObjectRef();
      logger.fine("Seeing if " + objRef + " refers to an import.");

      SSAInstruction def = this.du.getDef(objRef);
      logger.finer("Found definition: " + def + ".");

      TypeName scriptTypeName = this.ir.getMethod().getReference().getDeclaringClass().getName();
      logger.finer("Found script: " + scriptTypeName + ".");

      String scriptName = getScriptName(scriptTypeName);
      logger.fine("Script name is: " + scriptName);
      assert scriptName.endsWith("." + PYTHON_FILE_EXTENSION);

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
                  + declaredTarget.getDeclaringClass().getName().toString().substring(1)
                  + " to wildcard imports for: "
                  + scriptName
                  + ".");

          // Add the library to the script's queue of wildcard imports.
          getBuilder()
              .getScriptToWildcardImports()
              .compute(
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
        String strippedFieldName = getStrippedDeclaredFieldName(getInstruction);

        MethodReference methodReference = getMethodReferenceRepresentingScript(strippedFieldName);

        logger.info(
            "Adding: "
                + methodReference.getDeclaringClass().getName().toString().substring(1)
                + " to wildcard imports for: "
                + scriptName
                + ".");

        // Add the script to the queue of this script's wildcard imports.
        getBuilder()
            .getScriptToWildcardImports()
            .compute(
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
      } else if (def instanceof AstPropertyRead) processWildcardImports((AstPropertyRead) def);
      else
        throw new IllegalArgumentException(
            "Not expecting the definition: "
                + def
                + " of the object reference of: "
                + instruction
                + " to be: "
                + def.getClass());
    }

    /**
     * Given a script's name, returns the {@link MethodReference} representing the script.
     *
     * @param scriptName The name of the script.
     * @return The corresponding {@link MethodReference} representing the script.
     */
    private static MethodReference getMethodReferenceRepresentingScript(String scriptName) {
      TypeReference typeReference =
          TypeReference.findOrCreate(PythonTypes.pythonLoader, "L" + scriptName);

      return MethodReference.findOrCreate(
          typeReference,
          Atom.findOrCreateAsciiAtom("do"),
          Descriptor.findOrCreate(null, PythonTypes.rootTypeName));
    }

    @Override
    public void visitAstGlobalRead(AstGlobalRead globalRead) {
      super.visitAstGlobalRead(globalRead);

      TypeName enclosingMethodTypeName =
          this.ir.getMethod().getReference().getDeclaringClass().getName();

      String scriptName = getScriptName(enclosingMethodTypeName);

      if (scriptName.endsWith("." + PYTHON_FILE_EXTENSION)) {
        // We have a valid script name.
        logger.fine("Script name is: " + scriptName);
        String fieldName = getStrippedDeclaredFieldName(globalRead);
        try {
          processWildcardImports(globalRead, scriptName, fieldName);
        } catch (CancelException e) {
          throw new CancelRuntimeException(e);
        }
      }
    }

    /**
     * Returns the name of the script for the given {@link TypeName} representing a the name of a
     * method.
     *
     * @param methodName The name of the method.
     * @return The name of the corresponding script.
     * @implNote In Ariadne, scripts are also "methods" with the name "do."
     */
    private static String getScriptName(TypeName methodName) {
      boolean script = methodName.toString().endsWith(PYTHON_FILE_EXTENSION);

      if (script)
        return methodName.getPackage() == null
            ? methodName.getClassName().toString()
            : methodName.getPackage().toString() + "/" + methodName.getClassName().toString();
      else
        return (methodName.getPackage() == null
                ? methodName.getClassName()
                : methodName.getPackage())
            .toString();
    }

    /**
     * Processes the given {@link SSAInstruction} for any potential wildcard imports being utilized
     * by the instruction.
     *
     * @param instruction The {@link SSAInstruction} whose definition may depend on a wildcard
     *     import.
     * @param scriptName The name of the script to check for wildcard imports.
     * @param fieldName The name of the field that may be imported using a wildcard.
     */
    private void processWildcardImports(
        SSAInstruction instruction, String scriptName, String fieldName) throws CancelException {
      // Get the method reference for the given script.
      MethodReference reference = getMethodReferenceRepresentingScript(scriptName);

      // Get the nodes for the script.
      Set<CGNode> scriptNodes = this.getBuilder().getCallGraph().getNodes(reference);

      // For each node representing the script.
      for (CGNode node : scriptNodes) {
        // if we haven't visited the node yet.
        if (!this.getBuilder().haveAlreadyVisited(node)) {
          // visit the node first. Otherwise, we won't know if there are any wildcard imports in
          // it.
          this.getBuilder().addConstraintsFromNode(node, null);

          assert this.getBuilder().haveAlreadyVisited(node);
        }
      }

      // Are there any wildcard imports for this script?
      if (getBuilder().getScriptToWildcardImports().containsKey(scriptName)) {
        logger.info("Found wildcard imports in " + scriptName + " for " + instruction + ".");

        Deque<MethodReference> deque = getBuilder().getScriptToWildcardImports().get(scriptName);

        for (MethodReference importMethodReference : deque) {
          logger.fine(
              "Library with wildcard import is: "
                  + importMethodReference.getDeclaringClass().getName().toString().substring(1)
                  + ".");

          logger.fine("Examining global: " + fieldName + " for wildcard import.");

          CallGraph callGraph = this.getBuilder().getCallGraph();
          Set<CGNode> nodes = callGraph.getNodes(importMethodReference);

          if (nodes.isEmpty())
            throw new IllegalStateException(
                "Can't find CG node for import method: "
                    + importMethodReference.getSignature()
                    + ".");

          PointerKey defPK = this.getPointerKeyForLocal(instruction.getDef());
          assert defPK != null;

          for (CGNode n : nodes) {
            for (Iterator<NewSiteReference> nit = n.iterateNewSites(); nit.hasNext(); ) {
              NewSiteReference newSiteReference = nit.next();

              String name = newSiteReference.getDeclaredType().getName().getClassName().toString();
              logger.finest("Examining: " + name + ".");

              if (name.equals(fieldName)) {
                logger.info("Found wildcard import for: " + name + ".");

                InstanceKey instanceKey =
                    this.getBuilder().getInstanceKeyForAllocation(n, newSiteReference);

                if (this.system.newConstraint(defPK, instanceKey)) {
                  logger.fine("Added constraint that: " + defPK + " gets: " + instanceKey + ".");
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

                        if (fieldName.equals(putField.getName().toString())) {
                          // Found it.
                          int putVal = putInstruction.getVal();

                          // Make the def point to the put instruction value.
                          PointerKey putValPK = getBuilder().getPointerKeyForLocal(n, putVal);

                          if (system.newConstraint(defPK, assignOperator, putValPK))
                            logger.fine(
                                "Added constraint that: " + defPK + " gets: " + putValPK + ".");
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
      return declaredFieldName.substring(
          (GLOBAL_IDENTIFIER + " ").length(), declaredFieldName.length());
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

  /**
   * A mapping of script names to wildcard imports included in the script.
   *
   * @return A mapping of script names to wildcard imports included in the corresponding script.
   */
  protected Map<String, Deque<MethodReference>> getScriptToWildcardImports() {
    return scriptToWildcardImports;
  }
}
