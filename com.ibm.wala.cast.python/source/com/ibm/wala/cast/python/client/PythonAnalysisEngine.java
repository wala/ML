package com.ibm.wala.cast.python.client;

import com.ibm.wala.cast.ipa.callgraph.AstCFAPointerKeys;
import com.ibm.wala.cast.ipa.callgraph.AstContextInsensitiveSSAContextInterpreter;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.loader.AstDynamicField;
import com.ibm.wala.cast.python.ipa.callgraph.PythonConstructorTargetSelector;
import com.ibm.wala.cast.python.ipa.callgraph.PythonSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.python.ipa.callgraph.PythonScopeMappingInstanceKeys;
import com.ibm.wala.cast.python.ipa.callgraph.PythonTrampolineTargetSelector;
import com.ibm.wala.cast.python.ipa.summaries.BuiltinFunctions;
import com.ibm.wala.cast.python.ipa.summaries.PythonComprehensionTrampolines;
import com.ibm.wala.cast.python.ipa.summaries.PythonSuper;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.loader.PythonLoaderFactory;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.python.util.PythonInterpreter;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cast.util.Util;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.ClassTargetSelector;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyClassTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyMethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ContextInsensitiveSelector;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.cha.SeqClassHierarchyFactory;
import com.ibm.wala.ipa.summaries.BypassClassTargetSelector;
import com.ibm.wala.ipa.summaries.BypassMethodTargetSelector;
import com.ibm.wala.ipa.summaries.BypassSyntheticClassLoader;
import com.ibm.wala.ipa.summaries.XMLMethodSummaryReader;
import com.ibm.wala.shrike.shrikeBT.Constants;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAOptions.DefaultValues;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class PythonAnalysisEngine<T>
    extends AbstractAnalysisEngine<InstanceKey, PythonSSAPropagationCallGraphBuilder, T> {

  private static final Logger logger = Logger.getLogger(PythonAnalysisEngine.class.getName());

  static {
    try {
      Class<?> j3 = Class.forName("com.ibm.wala.cast.python.loader.Python3LoaderFactory");
      PythonAnalysisEngine.setLoaderFactory((Class<? extends PythonLoaderFactory>) j3);
      Class<?> i3 = Class.forName("com.ibm.wala.cast.python.util.Python3Interpreter");
      PythonInterpreter.setInterpreter((PythonInterpreter) i3.newInstance());
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      try {
        Class<?> j2 = Class.forName("com.ibm.wala.cast.python.loader.Python2LoaderFactory");
        PythonAnalysisEngine.setLoaderFactory((Class<? extends PythonLoaderFactory>) j2);
        Class<?> i2 = Class.forName("com.ibm.wala.cast.python.util.Python2Interpreter");
        PythonInterpreter.setInterpreter((PythonInterpreter) i2.newInstance());
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e1) {
        assert false : e.getMessage() + ", then " + e1.getMessage();
      }
    }
  }

  private static Class<? extends PythonLoaderFactory> loaders;

  public static void setLoaderFactory(Class<? extends PythonLoaderFactory> lf) {
    loaders = lf;
  }

  protected PythonLoaderFactory loader;

  private final IRFactory<IMethod> irs = AstIRFactory.makeDefaultFactory();

  public PythonAnalysisEngine() {
    super();
    PythonLoaderFactory f;
    try {
      f = loaders.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      f = null;
      assert false : e.getMessage();
    }
    loader = f;
  }

  @Override
  public void buildAnalysisScope() throws IOException {
    scope =
        new AnalysisScope(Collections.singleton(PythonLanguage.Python)) {
          {
            loadersByName.put(PythonTypes.pythonLoaderName, PythonTypes.pythonLoader);
            loadersByName.put(
                SYNTHETIC,
                new ClassLoaderReference(
                    SYNTHETIC, PythonLanguage.Python.getName(), PythonTypes.pythonLoader));
          }
        };

    for (Module o : moduleFiles) {
      scope.addToScope(PythonTypes.pythonLoader, o);
    }
  }

  @Override
  public IClassHierarchy buildClassHierarchy() {
    IClassHierarchy cha = null;

    try {
	  cha = SeqClassHierarchyFactory.make(scope, loader);
    } catch (ClassHierarchyException e) {
      assert false : e;
      return null;
    }

    try {
      Util.checkForFrontEndErrors(cha);
    } catch (WalaException e) {
      logger.log(Level.WARNING, e, () -> "Encountered WALA exception, most likely from front-end parsing errors.");
    }

    setClassHierarchy(cha);
    return cha;
  }

  protected void addSummaryBypassLogic(AnalysisOptions options, String summary) {
    IClassHierarchy cha = getClassHierarchy();
    XMLMethodSummaryReader xml =
        new XMLMethodSummaryReader(getClass().getClassLoader().getResourceAsStream(summary), scope);
    for (TypeReference t : xml.getAllocatableClasses()) {
      BypassSyntheticClassLoader ldr =
          (BypassSyntheticClassLoader) cha.getLoader(scope.getSyntheticLoader());
      ldr.registerClass(
          t.getName(),
          new SyntheticClass(t, cha) {
            private final Map<Atom, IField> fields = HashMapFactory.make();

            @Override
            public IClassLoader getClassLoader() {
              return cha.getLoader(cha.getScope().getSyntheticLoader());
            }

            @Override
            public boolean isPublic() {
              return true;
            }

            @Override
            public boolean isPrivate() {
              return false;
            }

            @Override
            public int getModifiers() throws UnsupportedOperationException {
              return Constants.ACC_PUBLIC;
            }

            @Override
            public IClass getSuperclass() {
              return cha.lookupClass(PythonTypes.CodeBody);
            }

            @Override
            public Collection<? extends IClass> getDirectInterfaces() {
              return Collections.emptySet();
            }

            @Override
            public Collection<IClass> getAllImplementedInterfaces() {
              return Collections.emptySet();
            }

            @Override
            public IMethod getMethod(Selector selector) {
              // TODO Auto-generated method stub
              return null;
            }

            @Override
            public IField getField(Atom name) {
              if (!fields.containsKey(name)) {
                fields.put(
                    name,
                    new AstDynamicField(
                        false, cha.lookupClass(PythonTypes.Root), name, PythonTypes.Root));
              }
              return fields.get(name);
            }

            @Override
            public IMethod getClassInitializer() {
              // TODO Auto-generated method stub
              return null;
            }

            @Override
            public Collection<? extends IMethod> getDeclaredMethods() {
              // TODO Auto-generated method stub
              return null;
            }

            @Override
            public Collection<IField> getAllInstanceFields() {
              return fields.values();
            }

            @Override
            public Collection<IField> getAllStaticFields() {
              return Collections.emptySet();
            }

            @Override
            public Collection<IField> getAllFields() {
              return fields.values();
            }

            @Override
            public Collection<? extends IMethod> getAllMethods() {
              // TODO Auto-generated method stub
              return null;
            }

            @Override
            public Collection<IField> getDeclaredInstanceFields() {
              return fields.values();
            }

            @Override
            public Collection<IField> getDeclaredStaticFields() {
              return Collections.emptySet();
            }

            @Override
            public boolean isReferenceType() {
              return true;
            }
          });
    }

    MethodTargetSelector targetSelector = options.getMethodTargetSelector();
    targetSelector =
        new BypassMethodTargetSelector(
            targetSelector, xml.getSummaries(), xml.getIgnoredPackages(), cha);
    options.setSelector(targetSelector);

    ClassTargetSelector cs =
        new BypassClassTargetSelector(
            options.getClassTargetSelector(),
            xml.getAllocatableClasses(),
            cha,
            cha.getLoader(scope.getSyntheticLoader()));
    options.setSelector(cs);
  }

  protected void addBypassLogic(IClassHierarchy cha, AnalysisOptions options) {
    options.setSelector(
        new PythonTrampolineTargetSelector(
            new PythonConstructorTargetSelector(
                new PythonComprehensionTrampolines(options.getMethodTargetSelector()))));

    BuiltinFunctions builtins = new BuiltinFunctions(cha);
    options.setSelector(builtins.builtinClassTargetSelector(options.getClassTargetSelector()));

    addSummaryBypassLogic(options, "flask.xml");
    addSummaryBypassLogic(options, "pandas.xml");
    addSummaryBypassLogic(options, "functools.xml");
  }

  @Override
  protected Iterable<Entrypoint> makeDefaultEntrypoints(IClassHierarchy cha) {
    Set<Entrypoint> result = HashSetFactory.make();
    cha.forEach(
        entry -> {
          if (entry.getName().toString().endsWith(".py")) {
            MethodReference er =
                MethodReference.findOrCreate(entry.getReference(), AstMethodReference.fnSelector);
            result.add(new DefaultEntrypoint(er, cha));
          }
        });
    return result;
  }

  @Override
  protected PythonSSAPropagationCallGraphBuilder getCallGraphBuilder(
      IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache2) {
    IAnalysisCacheView cache = new AnalysisCacheImpl(irs, options.getSSAOptions());

    options.setSelector(new ClassHierarchyClassTargetSelector(cha));
    options.setSelector(new ClassHierarchyMethodTargetSelector(cha));

    addBypassLogic(cha, options);

    options.setUseConstantSpecificKeys(true);

    SSAOptions ssaOptions = options.getSSAOptions();
    ssaOptions.setDefaultValues(
        new DefaultValues() {
          @Override
          public int getDefaultValue(SymbolTable symtab, int valueNumber) {
            return symtab.getNullConstant();
          }
        });
    options.setSSAOptions(ssaOptions);

    PythonSSAPropagationCallGraphBuilder builder = makeBuilder(cha, options, cache);

    AstContextInsensitiveSSAContextInterpreter interpreter =
        new AstContextInsensitiveSSAContextInterpreter(options, cache);
    builder.setContextInterpreter(interpreter);

    builder.setContextSelector(new nCFAContextSelector(1, new ContextInsensitiveSelector()));

    builder.setInstanceKeys(
        new PythonScopeMappingInstanceKeys(
            builder,
            new ZeroXInstanceKeys(options, cha, interpreter, ZeroXInstanceKeys.ALLOCATIONS)));

    new PythonSuper(cha).handleSuperCalls(builder, options);

    return builder;
  }

  protected PythonSSAPropagationCallGraphBuilder makeBuilder(
      IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache) {
    return new PythonSSAPropagationCallGraphBuilder(cha, options, cache, new AstCFAPointerKeys());
  }

  public abstract T performAnalysis(PropagationCallGraphBuilder builder) throws CancelException;
}
