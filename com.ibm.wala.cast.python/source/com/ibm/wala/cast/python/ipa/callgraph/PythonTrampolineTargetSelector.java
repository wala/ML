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

import com.ibm.wala.cast.loader.DynamicCallSiteReference;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ipa.summaries.PythonInstanceMethodTrampoline;
import com.ibm.wala.cast.python.ipa.summaries.PythonSummarizedFunction;
import com.ibm.wala.cast.python.ipa.summaries.PythonSummary;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.NormalAllocationInNode;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.OrdinalSet;
import java.util.Map;

public class PythonTrampolineTargetSelector<T> implements MethodTargetSelector {
  private static final String CALL = "__call__";

  private final MethodTargetSelector base;

  private PythonAnalysisEngine<T> engine;

  public PythonTrampolineTargetSelector(
      MethodTargetSelector base, PythonAnalysisEngine<T> pythonAnalysisEngine) {
    this.base = base;
    this.engine = pythonAnalysisEngine;
  }

  private final Map<Pair<IClass, Integer>, IMethod> codeBodies = HashMapFactory.make();

  @SuppressWarnings("unchecked")
  @Override
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    if (receiver != null) {
      IClassHierarchy cha = receiver.getClassHierarchy();
      final boolean callable = receiver.getReference().equals(PythonTypes.object);

      if (cha.isSubclassOf(receiver, cha.lookupClass(PythonTypes.trampoline)) || callable) {
        PythonInvokeInstruction call = (PythonInvokeInstruction) caller.getIR().getCalls(site)[0];

        if (callable) {
          // It's a callable. Change the receiver.
          receiver = getCallable(caller, cha, call);

          if (receiver == null) return null; // not found.
        }

        Pair<IClass, Integer> key = Pair.make(receiver, call.getNumberOfTotalParameters());
        if (!codeBodies.containsKey(key)) {
          Map<Integer, Atom> names = HashMapFactory.make();
          MethodReference tr =
              MethodReference.findOrCreate(
                  receiver.getReference(),
                  Atom.findOrCreateUnicodeAtom("trampoline" + call.getNumberOfTotalParameters()),
                  AstMethodReference.fnDesc);
          PythonSummary x = new PythonSummary(tr, call.getNumberOfTotalParameters());
          IClass filter = ((PythonInstanceMethodTrampoline) receiver).getRealClass();
          int v = call.getNumberOfTotalParameters() + 1;
          x.addStatement(
              PythonLanguage.Python.instructionFactory()
                  .GetInstruction(
                      0,
                      v,
                      1,
                      FieldReference.findOrCreate(
                          PythonTypes.Root,
                          Atom.findOrCreateUnicodeAtom("$function"),
                          PythonTypes.Root)));
          int v0 = v + 1;
          x.addStatement(
              PythonLanguage.Python.instructionFactory()
                  .CheckCastInstruction(1, v0, v, filter.getReference(), true));
          int v1 = v + 2;
          x.addStatement(
              PythonLanguage.Python.instructionFactory()
                  .GetInstruction(
                      1,
                      v1,
                      1,
                      FieldReference.findOrCreate(
                          PythonTypes.Root,
                          Atom.findOrCreateUnicodeAtom("$self"),
                          PythonTypes.Root)));

          int i = 0;
          int[] params = new int[Math.max(2, call.getNumberOfPositionalParameters() + 1)];
          params[i++] = v0;
          params[i++] = v1;
          for (int j = 1; j < call.getNumberOfPositionalParameters(); j++) {
            params[i++] = j + 1;
          }

          int ki = 0, ji = call.getNumberOfPositionalParameters() + 1;
          Pair<String, Integer>[] keys = new Pair[0];
          if (call.getKeywords() != null) {
            keys = new Pair[call.getKeywords().size()];
            for (String k : call.getKeywords()) {
              names.put(ji, Atom.findOrCreateUnicodeAtom(k));
              keys[ki++] = Pair.make(k, ji++);
            }
          }

          int result = v1 + 1;
          int except = v1 + 2;
          CallSiteReference ref =
              new DynamicCallSiteReference(call.getCallSite().getDeclaredTarget(), 2);
          x.addStatement(new PythonInvokeInstruction(2, result, except, ref, params, keys));

          x.addStatement(new SSAReturnInstruction(3, result, false));

          x.setValueNames(names);

          codeBodies.put(key, new PythonSummarizedFunction(tr, x, receiver));
        }

        return codeBodies.get(key);
      }
    }

    return base.getCalleeTarget(caller, site, receiver);
  }

  private IClass getCallable(CGNode caller, IClassHierarchy cha, PythonInvokeInstruction call) {
    PythonSSAPropagationCallGraphBuilder builder = this.getEngine().getCachedCallGraphBuilder();

    // Lookup the __call__ method.
    PointerKeyFactory pkf = builder.getPointerKeyFactory();
    PointerKey receiver = pkf.getPointerKeyForLocal(caller, call.getUse(0));
    OrdinalSet<InstanceKey> objs = builder.getPointerAnalysis().getPointsToSet(receiver);
    for (InstanceKey o : objs) {
      NormalAllocationInNode instanceKey = (NormalAllocationInNode) o;
      CGNode node = instanceKey.getNode();
      IMethod method = node.getMethod();
      IClass declaringClass = method.getDeclaringClass();
      TypeName declaringClassName = declaringClass.getName();
      final String packageName = "$" + declaringClassName.toString().substring(1);
      TypeReference typeReference =
          TypeReference.findOrCreateClass(
              declaringClass.getClassLoader().getReference(), packageName, CALL);
      return cha.lookupClass(typeReference);
    }

    return null;
  }

  public PythonAnalysisEngine<T> getEngine() {
    return engine;
  }
}
