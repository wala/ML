package com.ibm.wala.cast.python.client;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.ipa.callgraph.AstCFAPointerKeys;
import com.ibm.wala.cast.ipa.callgraph.AstContextInsensitiveSSAContextInterpreter;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.loader.AstDynamicField;
import com.ibm.wala.cast.python.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.ipa.callgraph.PythonConstructorTargetSelector;
import com.ibm.wala.cast.python.ipa.callgraph.PythonSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.python.ipa.callgraph.PythonScopeMappingInstanceKeys;
import com.ibm.wala.cast.python.ipa.callgraph.PythonTrampolineTargetSelector;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.loader.PythonLoaderFactory;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.python.types.TensorType;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ClassTargetSelector;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyClassTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyMethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ContextInsensitiveSelector;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.ReturnValueKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.cha.SeqClassHierarchyFactory;
import com.ibm.wala.ipa.summaries.BypassClassTargetSelector;
import com.ibm.wala.ipa.summaries.BypassMethodTargetSelector;
import com.ibm.wala.ipa.summaries.BypassSyntheticClassLoader;
import com.ibm.wala.ipa.summaries.XMLMethodSummaryReader;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.strings.Atom;

public class PythonAnalysisEngine extends AbstractAnalysisEngine<InstanceKey, PythonSSAPropagationCallGraphBuilder, TensorTypeAnalysis> {
	private static final MethodReference reshape = MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName("Ltensorflow/functions/reshape")), AstMethodReference.fnSelector);

	private final PythonLoaderFactory loader = new PythonLoaderFactory();
	private final IRFactory<IMethod> irs = AstIRFactory.makeDefaultFactory();

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

	private void addBypassLogic(AnalysisOptions options) {
		IClassHierarchy cha = getClassHierarchy();
		Map<Atom,IField> fields = HashMapFactory.make();

		XMLMethodSummaryReader xml = new XMLMethodSummaryReader(getClass().getClassLoader().getResourceAsStream("tensorflow.xml"), scope);
		for(TypeReference t : xml.getAllocatableClasses()) {
			BypassSyntheticClassLoader ldr = (BypassSyntheticClassLoader) cha.getLoader(scope.getSyntheticLoader());
			ldr.registerClass(t.getName(), new SyntheticClass(t, cha) {
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
					if (! fields.containsKey(name)) {
						fields.put(name, new AstDynamicField(false, cha.lookupClass(PythonTypes.Root), name, PythonTypes.Root));
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
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Collection<IField> getAllStaticFields() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Collection<IField> getAllFields() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Collection<? extends IMethod> getAllMethods() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Collection<IField> getDeclaredInstanceFields() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Collection<IField> getDeclaredStaticFields() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public boolean isReferenceType() {
					// TODO Auto-generated method stub
					return false;
				}				
			});
		}

		MethodTargetSelector targetSelector = 
			new PythonTrampolineTargetSelector(
				new PythonConstructorTargetSelector(
					options.getMethodTargetSelector()));
		targetSelector = new BypassMethodTargetSelector(targetSelector, xml.getSummaries(), xml.getIgnoredPackages(), cha);
		options.setSelector(targetSelector);

		ClassTargetSelector cs = 
			new BypassClassTargetSelector(options.getClassTargetSelector(), 
					xml.getAllocatableClasses(), 
					cha, 
					cha.getLoader(scope.getSyntheticLoader()));
		options.setSelector(cs);
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
	protected PythonSSAPropagationCallGraphBuilder getCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options,
			IAnalysisCacheView cache2) {
		IAnalysisCacheView cache = new AnalysisCacheImpl(irs, options.getSSAOptions());
		
		options.setSelector(new ClassHierarchyClassTargetSelector(cha));
		options.setSelector(new ClassHierarchyMethodTargetSelector(cha));
		
		addBypassLogic(options);
		
		PythonSSAPropagationCallGraphBuilder builder = 
				new PythonSSAPropagationCallGraphBuilder(cha, options, cache, new AstCFAPointerKeys());

		AstContextInsensitiveSSAContextInterpreter interpreter = new AstContextInsensitiveSSAContextInterpreter(options, cache);
		builder.setContextInterpreter(interpreter);

		builder.setContextSelector(new ContextInsensitiveSelector());

		builder.setInstanceKeys(new PythonScopeMappingInstanceKeys(builder, new ZeroXInstanceKeys(options, cha, interpreter, ZeroXInstanceKeys.ALLOCATIONS)));

		return builder;
	}

	private static Set<PointsToSetVariable> getDataflowSources(Graph<PointsToSetVariable> dataflow) {
		Set<PointsToSetVariable> sources = HashSetFactory.make();
		for(PointsToSetVariable src : dataflow) {
			PointerKey k = src.getPointerKey();
			if (k instanceof LocalPointerKey) {
				LocalPointerKey kk = (LocalPointerKey)k;
				int vn = kk.getValueNumber();
				DefUse du = kk.getNode().getDU();
				SSAInstruction inst = du.getDef(vn);
				if (inst instanceof SSAInvokeInstruction) {
					SSAInvokeInstruction ni = (SSAInvokeInstruction) inst;
					if (ni.getCallSite().getDeclaredTarget().getName().toString().equals("read_data") && ni.getException() != vn) {
						sources.add(src);
					}
				}
			}
		}
		return sources;
	}

	private Map<PointsToSetVariable,TensorType> getReshapeTypes(PropagationCallGraphBuilder builder) {
		Map<PointsToSetVariable,TensorType> targets = HashMapFactory.make();
		for(CGNode n : builder.getCallGraph()) {
			if (n.getMethod().getReference().equals(reshape)) {
				for(Iterator<CGNode> srcs = builder.getCallGraph().getPredNodes(n); srcs.hasNext(); ) {
					CGNode src = srcs.next();
					for(Iterator<CallSiteReference> sites = builder.getCallGraph().getPossibleSites(src, n); sites.hasNext(); ) {
						CallSiteReference site = sites.next();
						for(SSAAbstractInvokeInstruction call : src.getIR().getCalls(site)) {
							targets.put(
								builder.getPropagationSystem().findOrCreatePointsToSet(builder.getPointerAnalysis().getHeapModel().getPointerKeyForReturnValue(n)),
								TensorType.reshapeArg(src, call.getUse(2)));
						}
					}
				}
			}
		}
		return targets;
	}
	
	@Override
	public TensorTypeAnalysis performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
		Graph<PointsToSetVariable> dataflow = SlowSparseNumberedGraph.duplicate(builder.getSystem().getFlowGraphIncludingImplicitConstraints());

		Set<PointsToSetVariable> sources = getDataflowSources(dataflow);
		System.err.println(sources);
		
		TensorType mnistData = TensorType.mnistInput();
		Map<PointsToSetVariable, TensorType> init = HashMapFactory.make();
		for(PointsToSetVariable v : sources) {
			init.put(v, mnistData);
		}
		
		Map<PointsToSetVariable, TensorType> reshapeTypes = getReshapeTypes(builder);			
		for(PointsToSetVariable to : reshapeTypes.keySet()) {
			assert to.getPointerKey() instanceof ReturnValueKey;
			PointerKey from = builder.getPointerAnalysis().getHeapModel().getPointerKeyForLocal(((ReturnValueKey)to.getPointerKey()).getNode(), 2);
			dataflow.addEdge(builder.getPropagationSystem().findOrCreatePointsToSet(from), to);
		}
		
		TensorTypeAnalysis tt = new TensorTypeAnalysis(dataflow, init, reshapeTypes);
		
		tt.solve(new NullProgressMonitor());
		
		return tt;
	}
}
