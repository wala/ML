package com.ibm.wala.cast.python.client;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import com.ibm.wala.cast.ipa.callgraph.AstCFAPointerKeys;
import com.ibm.wala.cast.ipa.callgraph.AstContextInsensitiveSSAContextInterpreter;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.python.ipa.callgraph.PythonConstructorTargetSelector;
import com.ibm.wala.cast.python.ipa.callgraph.PythonSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.python.ipa.callgraph.PythonScopeMappingInstanceKeys;
import com.ibm.wala.cast.python.ipa.callgraph.PythonTrampolineTargetSelector;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.loader.PythonLoaderFactory;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
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
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAOptions.DefaultValues;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;

public abstract class PythonAnalysisEngine<T>
		extends AbstractAnalysisEngine<InstanceKey, PythonSSAPropagationCallGraphBuilder, T> {

	private final PythonLoaderFactory loader = new PythonLoaderFactory();
	private final IRFactory<IMethod> irs = AstIRFactory.makeDefaultFactory();

	public PythonAnalysisEngine() {
		super();
	}

	@Override
	public void buildAnalysisScope() throws IOException {
		scope = new AnalysisScope(Collections.singleton(PythonLanguage.Python)) { 
			{
				loadersByName.put(PythonTypes.pythonLoaderName, PythonTypes.pythonLoader);
				loadersByName.put(SYNTHETIC, new ClassLoaderReference(SYNTHETIC, PythonLanguage.Python.getName(), PythonTypes.pythonLoader));
			}
		};
		
		for(Module o : moduleFiles) {
			scope.addToScope(PythonTypes.pythonLoader, o);			
		}
	}

	@Override
	public IClassHierarchy buildClassHierarchy() {
		try {
			IClassHierarchy cha = SeqClassHierarchyFactory.make(scope, loader);
			setClassHierarchy(cha);
			return cha;
		} catch (ClassHierarchyException e) {
			assert false : e;
			return null;
		}
	}

	protected void addBypassLogic(AnalysisOptions options) {
		options.setSelector(
			new PythonTrampolineTargetSelector(
				new PythonConstructorTargetSelector(
					options.getMethodTargetSelector())));
	}

	private String scriptName(Module m) {
		String path = ((ModuleEntry)m).getName();
		return "Lscript " + (path.contains("/")? path.substring(path.lastIndexOf('/')+1): path);
	}

	@Override
	protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
		Set<Entrypoint> result = HashSetFactory.make();
		for(Module m : moduleFiles) {
			IClass entry = cha.lookupClass(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.findOrCreate(scriptName(m))));
			assert entry != null: "bad root name " + scriptName(m) + ":\n" + cha;
			MethodReference er = MethodReference.findOrCreate(entry.getReference(), AstMethodReference.fnSelector);
			result.add(new DefaultEntrypoint(er, cha));
		}
		return result;
	}

	@Override
	protected PythonSSAPropagationCallGraphBuilder getCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache2) {
		IAnalysisCacheView cache = new AnalysisCacheImpl(irs, options.getSSAOptions());
		
		options.setSelector(new ClassHierarchyClassTargetSelector(cha));
		options.setSelector(new ClassHierarchyMethodTargetSelector(cha));
		
		addBypassLogic(options);
		
		options.setUseConstantSpecificKeys(true);
		
		SSAOptions ssaOptions = options.getSSAOptions();
		ssaOptions.setDefaultValues(new DefaultValues() {
			@Override
			public int getDefaultValue(SymbolTable symtab, int valueNumber) {
				return symtab.getNullConstant();
			} 
		});
		options.setSSAOptions(ssaOptions);
		
		PythonSSAPropagationCallGraphBuilder builder = 
			new PythonSSAPropagationCallGraphBuilder(cha, options, cache, new AstCFAPointerKeys());
	
		AstContextInsensitiveSSAContextInterpreter interpreter = new AstContextInsensitiveSSAContextInterpreter(options, cache);
		builder.setContextInterpreter(interpreter);
	
		builder.setContextSelector(new nCFAContextSelector(1, new ContextInsensitiveSelector()));
	
		builder.setInstanceKeys(new PythonScopeMappingInstanceKeys(builder, new ZeroXInstanceKeys(options, cha, interpreter, ZeroXInstanceKeys.ALLOCATIONS)));
	
		return builder;
	}

	public abstract T performAnalysis(PropagationCallGraphBuilder builder) throws CancelException;

}