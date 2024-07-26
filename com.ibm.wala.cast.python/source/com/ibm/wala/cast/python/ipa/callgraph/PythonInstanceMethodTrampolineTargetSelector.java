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
import static com.ibm.wala.cast.python.util.Util.isClassMethod;
import static com.ibm.wala.types.annotations.Annotation.make;

import com.ibm.wala.cast.ipa.callgraph.ScopeMappingInstanceKeys.ScopeMappingInstanceKey;
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
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    // TODO: Callable detection may need to be moved. See https://github.com/wala/ML/issues/207. If
    // it stays here, we should further document the receiver swapping process.
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

    // The set of potential callables to be returned.
    Set<IClass> callableSet = new HashSet<>();

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

        callableSet.add(callable);
      }
    }

    // if there's only one possible option.
    if (callableSet.size() == 1) {
      IClass callable = callableSet.iterator().next();
      assert callable != null : "Callable should be non-null.";
      return callable;
    }

    // if we have multiple candidates.
    if (callableSet.size() > 1)
      // we cannot accurately select one.
      LOGGER.warning("Multiple (" + callableSet.size() + ") callable targets found.");

    return null;
  }

  /**
   * Extracts the {@link AllocationSiteInNode} from the given {@link InstanceKey}. If the given
   * {@link InstanceKey} is an instance of {@link AllocationSiteInNode}, then it itself is returned.
   * If the given {@link InstanceKey} is a {@link ScopeMappingInstanceKey}, then it's base {@link
   * InstanceKey} is returned if it is an instance {@link AllocationSiteInNode}.
   *
   * @param instanceKey The {@link InstanceKey} in question.
   * @return The {@link AllocationSiteInNode} corresponding to the given {@link InstanceKey}
   *     according to the above scheme.
   */
  private static AllocationSiteInNode getAllocationSiteInNode(InstanceKey instanceKey) {
    if (instanceKey instanceof AllocationSiteInNode) return (AllocationSiteInNode) instanceKey;
    else if (instanceKey instanceof ScopeMappingInstanceKey) {
      ScopeMappingInstanceKey smik = (ScopeMappingInstanceKey) instanceKey;
      InstanceKey baseInstanceKey = smik.getBase();

      if (baseInstanceKey instanceof AllocationSiteInNode)
        return (AllocationSiteInNode) baseInstanceKey;
      else if (baseInstanceKey instanceof ConstantKey) {
        return getAllocationSiteInNode((ConstantKey<?>) baseInstanceKey);
      } else
        throw new IllegalArgumentException(
            "Can't extract AllocationSiteInNode from: "
                + baseInstanceKey
                + ". Not expecting: "
                + baseInstanceKey.getClass()
                + ".");
    } else if (instanceKey instanceof ConstantKey) {
      return getAllocationSiteInNode((ConstantKey<?>) instanceKey);
    } else
      throw new IllegalArgumentException(
          "Can't extract AllocationSiteInNode from: "
              + instanceKey
              + ". Not expecting: "
              + instanceKey.getClass()
              + ".");
  }

  /**
   * If the given {@link ConstantKey}'s value is <code>null</code>, then issue a warning and return
   * <code>null</code>. Otherwise, throw an {@link IllegalArgumentException} stating that an {@link
   * AllocationSiteInNode} cannot be extracted from the given {@link ConstantKey}. A value of <code>
   * null</code> most likely indicates that a receiver can potentially be <code>null</code>.
   *
   * @param constantKey The {@link ConstantKey} from which to extract the corresponding {@link
   *     AllocationSiteInNode}.
   * @return <code>null</code> if the given {@link ConstantKey}'s value is <code>null</code>.
   * @throws IllegalArgumentException If the constant's value is another else other than <code>null
   *     </code>.
   */
  private static AllocationSiteInNode getAllocationSiteInNode(ConstantKey<?> constantKey) {
    Object value = constantKey.getValue();

    if (value == null) {
      LOGGER.warning("Can't extract AllocationSiteInNode from: " + constantKey + ".");
      return null;
    } else
      throw new IllegalArgumentException(
          "Can't extract AllocationSiteInNode from: "
              + constantKey
              + ". Not expecting value of: "
              + value
              + " from ConstantKey.");
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
