package com.ibm.wala.cast.python.ipa.callgraph;

import com.ibm.wala.cast.ipa.callgraph.ScopeMappingInstanceKeys;
import com.ibm.wala.cast.loader.AstMethod.LexicalParent;
import com.ibm.wala.cast.loader.CAstAbstractModuleLoader.DynamicMethodObject;
import com.ibm.wala.cast.python.ipa.summaries.PythonInstanceMethodTrampoline;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.collections.Pair;
import java.util.Collection;
import java.util.Collections;

public class PythonScopeMappingInstanceKeys extends ScopeMappingInstanceKeys {

  private final IClassHierarchy cha;
  private final IClass codeBody;

  public PythonScopeMappingInstanceKeys(
      PropagationCallGraphBuilder builder, InstanceKeyFactory basic) {
    super(builder, basic);
    this.cha = builder.getClassHierarchy();
    this.codeBody = cha.lookupClass(PythonTypes.CodeBody);
  }

  protected LexicalParent[] getParents(InstanceKey base) {
    IClass cls = base.getConcreteType();

    if (cls instanceof PythonInstanceMethodTrampoline) {
      cls = ((PythonInstanceMethodTrampoline) cls).getRealClass();
    }

    IMethod function = cls.getMethod(AstMethodReference.fnSelector);

    return function == null || !(function instanceof DynamicMethodObject)
        ? new LexicalParent[0]
        : ((DynamicMethodObject) function).getParents();
  }

  @Override
  protected boolean needsScopeMappingKey(InstanceKey base) {
    return cha.isSubclassOf(base.getConcreteType(), codeBody) && getParents(base).length > 0;
  }

  @Override
  protected Collection<CGNode> getConstructorCallers(
      ScopeMappingInstanceKey smik, Pair<String, String> name) {
    return Collections.singleton(smik.getCreator());
  }
}
