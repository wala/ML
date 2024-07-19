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

import static com.ibm.wala.cast.python.types.PythonTypes.STATIC_METHOD;
import static com.ibm.wala.cast.python.types.Util.getDeclaringClassTypeReference;
import static com.ibm.wala.cast.python.util.Util.getAllocationSiteInNode;
import static com.ibm.wala.cast.python.util.Util.isClassMethod;
import static com.ibm.wala.types.annotations.Annotation.make;

import com.ibm.wala.cast.loader.DynamicCallSiteReference;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ipa.summaries.PythonInstanceMethodTrampoline;
import com.ibm.wala.cast.python.ipa.summaries.PythonSummary;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.OrdinalSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class PythonInstanceMethodTrampolineTargetSelector<T>
    extends PythonMethodTrampolineTargetSelector<T> {

  private static final Logger LOGGER =
      Logger.getLogger(PythonInstanceMethodTrampolineTargetSelector.class.getName());

  /**
   * The method name that is used for Python callables.
   *
   * @see <a href="https://docs.python.org/3/reference/datamodel.html#class-instances">Python
   *     documentation</a>.
   */
  private static final String CALLABLE_METHOD_NAME = "__call__";

  /**
   * The method name that is used for tf.keras.Models callables. This is a workaround for
   * https://github.com/wala/ML/issues/106.
   *
   * @see <a
   *     href="https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/keras/Model#call">TensorFlow
   *     documentation</a>.
   */
  private static final String CALLABLE_METHOD_NAME_FOR_KERAS_MODELS = "call";

  private PythonAnalysisEngine<T> engine;

  public PythonInstanceMethodTrampolineTargetSelector(
      MethodTargetSelector base, PythonAnalysisEngine<T> engine) {
    super(base);
    this.engine = engine;
  }

  @Override
  protected boolean shouldProcess(CGNode caller, CallSiteReference site, IClass receiver) {
    IClassHierarchy cha = receiver.getClassHierarchy();
    return cha.isSubclassOf(receiver, cha.lookupClass(PythonTypes.trampoline))
        || this.isCallable(receiver);
  }

  @Override
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    if (isCallable(receiver)) {
      LOGGER.fine("Encountered callable.");

      PythonInvokeInstruction call = this.getCall(caller, site);

      // It's a callable. Change the receiver.
      receiver = getCallable(caller, receiver.getClassHierarchy(), call);

      if (receiver == null) return null; // not found.
      else LOGGER.fine("Substituting the receiver with one derived from a callable.");
    }

    return super.getCalleeTarget(caller, site, receiver);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void populate(
      PythonSummary x, int v, IClass receiver, PythonInvokeInstruction call, Logger logger) {
    Map<Integer, Atom> names = HashMapFactory.make();
    IClass filter = ((PythonInstanceMethodTrampoline) receiver).getRealClass();

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

    int v1;

    // Are we calling a static method?
    boolean staticMethodReceiver = filter.getAnnotations().contains(make(STATIC_METHOD));
    logger.fine(
        staticMethodReceiver
            ? "Found static method receiver: " + filter
            : "Method is not static: " + filter);

    // Are we calling a class method? If so, it would be using an object instance instead of a
    // class on the LHS.
    boolean classMethodReceiver = isClassMethod(receiver);

    // only add self if the receiver isn't static or a class method.
    if (!staticMethodReceiver && !classMethodReceiver) {
      v1 = v + 2;

      x.addStatement(
          PythonLanguage.Python.instructionFactory()
              .GetInstruction(
                  1,
                  v1,
                  1,
                  FieldReference.findOrCreate(
                      PythonTypes.Root, Atom.findOrCreateUnicodeAtom("$self"), PythonTypes.Root)));
    } else if (classMethodReceiver) {
      // Add a class reference.
      v1 = v + 2;

      x.addStatement(
          PythonLanguage.Python.instructionFactory()
              .GetInstruction(
                  1,
                  v1,
                  1,
                  FieldReference.findOrCreate(
                      PythonTypes.Root, Atom.findOrCreateUnicodeAtom("$class"), PythonTypes.Root)));

      int v2 = v + 3;
      TypeReference reference = getDeclaringClassTypeReference(filter.getReference());

      x.addStatement(
          PythonLanguage.Python.instructionFactory()
              .CheckCastInstruction(1, v2, v1++, reference, true));
    } else v1 = v + 1;

    int i = 0;
    int paramSize =
        Math.max(
            staticMethodReceiver ? 1 : 2,
            call.getNumberOfPositionalParameters() + (staticMethodReceiver ? 0 : 1));
    int[] params = new int[paramSize];
    params[i++] = v0;

    if (!staticMethodReceiver) params[i++] = v1;

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

    int result = v1 + 1;
    int except = v1 + 2;

    CallSiteReference ref = new DynamicCallSiteReference(call.getCallSite().getDeclaredTarget(), 2);

    x.addStatement(new PythonInvokeInstruction(2, result, except, ref, params, keys));
    x.addStatement(new SSAReturnInstruction(3, result, false));
    x.setValueNames(names);
  }

  /**
   * Returns the callable method of the receiver of the given {@link PythonInvokeInstruction}.
   *
   * @param caller The {@link CGNode} representing the caller of the given {@link
   *     PythonInvokeInstruction}.
   * @param cha The receiver's {@link IClassHierarchy}.
   * @param call The {@link PythonInvokeInstruction} in question.
   * @return The callable method the given {@link PythonInvokeInstruction}'s receiver.
   */
  private IClass getCallable(CGNode caller, IClassHierarchy cha, PythonInvokeInstruction call) {
    PythonSSAPropagationCallGraphBuilder builder = this.getEngine().getCachedCallGraphBuilder();

    // Lookup the callable method.
    PointerKeyFactory pkf = builder.getPointerKeyFactory();
    PointerKey receiver = pkf.getPointerKeyForLocal(caller, call.getUse(0));
    OrdinalSet<InstanceKey> objs = builder.getPointerAnalysis().getPointsToSet(receiver);

    Map<InstanceKey, IClass> instanceToCallable = new HashMap<>();

    for (InstanceKey o : objs) {
      AllocationSiteInNode instanceKey = getAllocationSiteInNode(o);
      if (instanceKey != null) {
        CGNode node = instanceKey.getNode();
        IMethod method = node.getMethod();
        IClass declaringClass = method.getDeclaringClass();
        final ClassLoaderReference classLoaderReference =
            declaringClass.getClassLoader().getReference();
        TypeName declaringClassName = declaringClass.getName();
        final String packageName = "$" + declaringClassName.toString().substring(1);

        IClass callable =
            cha.lookupClass(
                TypeReference.findOrCreateClass(
                    classLoaderReference, packageName, CALLABLE_METHOD_NAME));

        // TODO: Remove this code once https://github.com/wala/ML/issues/118 is completed.
        if (callable == null) {
          // try the workaround for https://github.com/wala/ML/issues/106. NOTE: We cannot verify
          // that the super class is tf.keras.Model due to https://github.com/wala/ML/issues/118.
          LOGGER.fine("Attempting callable workaround for https://github.com/wala/ML/issues/118.");

          callable =
              cha.lookupClass(
                  TypeReference.findOrCreateClass(
                      classLoaderReference, packageName, CALLABLE_METHOD_NAME_FOR_KERAS_MODELS));

          if (callable != null)
            LOGGER.info("Applying callable workaround for https://github.com/wala/ML/issues/118.");
        }

        if (callable != null) {
          if (instanceToCallable.containsKey(instanceKey))
            throw new IllegalStateException("Exisitng mapping found for: " + instanceKey);

          IClass previousValue = instanceToCallable.put(instanceKey, callable);
          assert previousValue == null : "Not expecting a previous mapping.";
        }
      }
    }

    // if there's only one possible option.
    if (instanceToCallable.values().size() == 1) {
      IClass callable = instanceToCallable.values().iterator().next();
      assert callable != null : "Callable should be non-null.";
      return callable;
    }

    // if we have multiple candidates.
    if (instanceToCallable.values().size() > 1)
      // we cannot accurately select one.
      LOGGER.warning(
          "Multiple (" + instanceToCallable.values().size() + ") callable targets found.");

    return null;
  }

  public PythonAnalysisEngine<T> getEngine() {
    return engine;
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  /**
   * Returns true iff the given {@link IClass} represents a Python callable object.
   *
   * @param receiver The {@link IClass} in question.
   * @return True iff the given {@link IClass} represents a Python callable object.
   */
  private boolean isCallable(IClass receiver) {
    return receiver != null && receiver.getReference().equals(PythonTypes.object);
  }
}
