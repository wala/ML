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

import static com.ibm.wala.cast.python.types.Util.getGlobalName;
import static com.ibm.wala.cast.python.types.Util.makeGlobalRef;

import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstInstructionFactory;
import com.ibm.wala.cast.loader.DynamicCallSiteReference;
import com.ibm.wala.cast.python.ipa.summaries.PythonInstanceMethodTrampoline;
import com.ibm.wala.cast.python.ipa.summaries.PythonSummarizedFunction;
import com.ibm.wala.cast.python.ipa.summaries.PythonSummary;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.loader.PythonLoader.PythonClass;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

public class PythonConstructorTargetSelector implements MethodTargetSelector {

  private static final Logger LOGGER =
      Logger.getLogger(PythonConstructorTargetSelector.class.getName());

  private final Map<IClass, IMethod> ctors = HashMapFactory.make();

  private final MethodTargetSelector base;

  public PythonConstructorTargetSelector(MethodTargetSelector base) {
    this.base = base;
  }

  @Override
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    if (receiver != null) {
      LOGGER.fine("Getting callee target for receiver: " + receiver);
      LOGGER.fine("Calling method name is: " + caller.getMethod().getName());

      IClassHierarchy cha = receiver.getClassHierarchy();
      if (cha.isSubclassOf(receiver, cha.lookupClass(PythonTypes.object))
          && receiver instanceof PythonClass) {
        if (!ctors.containsKey(receiver)) {
          TypeReference ctorRef =
              TypeReference.findOrCreate(
                  receiver.getClassLoader().getReference(), receiver.getName() + "/__init__");
          IClass ctorCls = cha.lookupClass(ctorRef);
          IMethod init = ctorCls == null ? null : ctorCls.getMethod(AstMethodReference.fnSelector);
          int params = init == null ? 1 : init.getNumberOfParameters();
          int v = params + 2;
          int pc = 0;
          int inst = v++;
          MethodReference ref =
              MethodReference.findOrCreate(
                  receiver.getReference(), site.getDeclaredTarget().getSelector());
          PythonSummary ctor = new PythonSummary(ref, params);
          AstInstructionFactory insts = PythonLanguage.Python.instructionFactory();
          ctor.addStatement(
              insts.NewInstruction(pc, inst, NewSiteReference.make(pc, PythonTypes.object)));
          pc++;

          PythonClass x = (PythonClass) receiver;
          for (TypeReference r : x.getInnerReferences()) {
            int orig_t = v++;
            String typeName = r.getName().toString();
            typeName = typeName.substring(typeName.lastIndexOf('/') + 1);
            FieldReference inner =
                FieldReference.findOrCreate(
                    PythonTypes.Root, Atom.findOrCreateUnicodeAtom(typeName), PythonTypes.Root);

            ctor.addStatement(insts.GetInstruction(pc, orig_t, 1, inner));
            pc++;

            ctor.addStatement(insts.PutInstruction(pc, inst, orig_t, inner));
            pc++;
          }

          for (MethodReference r : x.getMethodReferences()) {
            int f = v++;
            ctor.addStatement(
                insts.NewInstruction(
                    pc,
                    f,
                    NewSiteReference.make(
                        pc,
                        PythonInstanceMethodTrampoline.findOrCreate(
                            r.getDeclaringClass(), receiver.getClassHierarchy()))));
            pc++;

            ctor.addStatement(
                insts.PutInstruction(
                    pc,
                    f,
                    inst,
                    FieldReference.findOrCreate(
                        PythonTypes.Root,
                        Atom.findOrCreateUnicodeAtom("$self"),
                        PythonTypes.Root)));
            pc++;

            int orig_f = v++;
            ctor.addStatement(
                insts.GetInstruction(
                    pc,
                    orig_f,
                    1,
                    FieldReference.findOrCreate(PythonTypes.Root, r.getName(), PythonTypes.Root)));
            pc++;

            ctor.addStatement(
                insts.PutInstruction(
                    pc,
                    f,
                    orig_f,
                    FieldReference.findOrCreate(
                        PythonTypes.Root,
                        Atom.findOrCreateUnicodeAtom("$function"),
                        PythonTypes.Root)));
            pc++;

            // Add a metadata variable that refers to the declaring class.
            // NOTE: Per https://docs.python.org/3/library/functions.html#classmethod, "[i]f a class
            // method is called for a derived class, the derived class object is passed as the
            // implied first argument." I'm unsure whether `receiver` can refer to the derived
            // class especially in light of https://github.com/wala/ML/issues/107.
            int classVar = v++;
            String globalName = getGlobalName(r);
            FieldReference globalRef = makeGlobalRef(receiver.getClassLoader(), globalName);

            ctor.addStatement(new AstGlobalRead(pc++, classVar, globalRef));

            ctor.addStatement(
                insts.PutInstruction(
                    pc++,
                    f,
                    classVar,
                    FieldReference.findOrCreate(
                        PythonTypes.Root,
                        Atom.findOrCreateUnicodeAtom("$class"),
                        PythonTypes.Root)));

            ctor.addStatement(
                insts.PutInstruction(
                    pc,
                    inst,
                    f,
                    FieldReference.findOrCreate(PythonTypes.Root, r.getName(), PythonTypes.Root)));
            pc++;
          }

          for (IField f : ((PythonClass) receiver).getAllStaticFields()) {
            int tmp = v++;
            int name = v++;

            ctor.addConstant(name, new ConstantValue(f.getName().toString()));
            ctor.addStatement(insts.PropertyRead(pc++, tmp, 1, name));
            ctor.addStatement(insts.PropertyWrite(pc++, inst, name, tmp));
          }

          if (init != null) {
            int fv = v++;
            ctor.addStatement(
                insts.GetInstruction(
                    pc,
                    fv,
                    1,
                    FieldReference.findOrCreate(
                        PythonTypes.Root,
                        Atom.findOrCreateUnicodeAtom("__init__"),
                        PythonTypes.Root)));
            pc++;

            int numberOfParameters = init.getNumberOfParameters();
            int[] cps = new int[numberOfParameters > 1 ? numberOfParameters : 2];
            cps[0] = fv;
            cps[1] = inst;
            for (int j = 2; j < numberOfParameters; j++) {
              cps[j] = j;
            }

            int result = v++;
            int except = v++;
            CallSiteReference cref = new DynamicCallSiteReference(site.getDeclaredTarget(), pc);
            @SuppressWarnings("unchecked")
            Pair<String, Integer>[] keywordParams = new Pair[0];
            ctor.addStatement(
                new PythonInvokeInstruction(2, result, except, cref, cps, keywordParams));
            pc++;
          }

          ctor.addStatement(insts.ReturnInstruction(pc++, inst, false));

          ctor.setValueNames(Collections.singletonMap(1, Atom.findOrCreateUnicodeAtom("self")));

          ctors.put(receiver, new PythonSummarizedFunction(ref, ctor, receiver));
        }

        return ctors.get(receiver);
      }
    }
    return base.getCalleeTarget(caller, site, receiver);
  }
}
