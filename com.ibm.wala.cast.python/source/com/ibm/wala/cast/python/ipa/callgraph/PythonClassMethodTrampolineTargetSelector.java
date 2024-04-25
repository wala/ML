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
import static com.ibm.wala.cast.python.util.Util.isClassMethod;

import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.loader.DynamicCallSiteReference;
import com.ibm.wala.cast.python.ipa.summaries.PythonSummary;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A trampoline for <a href="https://docs.python.org/3/library/functions.html#classmethod">class
 * methods</a> that are not called using object instances.
 *
 * @author <a href="mailto:khatchad@hunter.cuny.edu">Raffi Khatchadourian</a>
 */
public class PythonClassMethodTrampolineTargetSelector<T>
    extends PythonMethodTrampolineTargetSelector<T> {

  protected static final Logger LOGGER =
      Logger.getLogger(PythonClassMethodTrampolineTargetSelector.class.getName());

  public PythonClassMethodTrampolineTargetSelector(MethodTargetSelector base) {
    super(base);
  }

  @Override
  protected boolean shouldProcess(CGNode caller, CallSiteReference site, IClass receiver) {
    IClassHierarchy cha = receiver.getClassHierarchy();

    // Are we calling a class method?
    boolean classMethodReceiver = isClassMethod(receiver);

    // Is the caller a trampoline?
    boolean trampoline =
        caller
            .getMethod()
            .getSelector()
            .getName()
            .startsWith(Atom.findOrCreateAsciiAtom("trampoline"));

    return classMethodReceiver
        && !cha.isSubclassOf(receiver, cha.lookupClass(PythonTypes.trampoline))
        && !trampoline;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void populate(
      PythonSummary x, int v, IClass receiver, PythonInvokeInstruction call, Logger logger) {
    Map<Integer, Atom> names = HashMapFactory.make();
    SSAInstructionFactory insts = PythonLanguage.Python.instructionFactory();

    // Read the class from the global scope.
    String globalName = getGlobalName(receiver.getReference());
    FieldReference globalRef = makeGlobalRef(receiver.getClassLoader(), globalName);
    int globalReadRes = v++;
    int pc = 0;

    x.addStatement(new AstGlobalRead(pc++, globalReadRes, globalRef));

    int getInstRes = v++;

    // Read the field from the class corresponding to the called method.
    FieldReference method =
        FieldReference.findOrCreate(
            PythonTypes.Root, Atom.findOrCreateUnicodeAtom("the_class_method"), PythonTypes.Root);

    x.addStatement(insts.GetInstruction(pc++, getInstRes, globalReadRes, method));

    int i = 0;
    int paramSize = Math.max(2, call.getNumberOfPositionalParameters() + 1);
    int[] params = new int[paramSize];
    params[i++] = getInstRes;
    params[i++] = globalReadRes;

    for (int j = 1; j < call.getNumberOfPositionalParameters(); j++) params[i++] = j + 1;

    int ki = 0, ji = call.getNumberOfPositionalParameters() + 1;
    Pair<String, Integer>[] keys = new Pair[0];

    if (call.getKeywords() != null) {
      keys = new Pair[call.getKeywords().size()];

      for (String k : call.getKeywords()) {
        names.put(ji, Atom.findOrCreateUnicodeAtom(k));
        keys[ki++] = Pair.make(k, ji++);
      }
    }

    CallSiteReference ref = new DynamicCallSiteReference(call.getCallSite().getDeclaredTarget(), 2);

    int except = v++;
    int invokeResult = v++;

    x.addStatement(new PythonInvokeInstruction(pc++, invokeResult, except, ref, params, keys));
    x.addStatement(new SSAReturnInstruction(pc++, invokeResult, false));
    x.setValueNames(names);
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }
}
