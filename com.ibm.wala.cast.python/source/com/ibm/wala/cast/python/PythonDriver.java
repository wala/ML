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
package com.ibm.wala.cast.python;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.ipa.callgraph.AstCFAPointerKeys;
import com.ibm.wala.cast.ipa.callgraph.AstContextInsensitiveSSAContextInterpreter;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.loader.AstDynamicField;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.python.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.ipa.callgraph.PythonConstructorTargetSelector;
import com.ibm.wala.cast.python.ipa.callgraph.PythonSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.python.ipa.callgraph.PythonScopeMappingInstanceKeys;
import com.ibm.wala.cast.python.ipa.callgraph.PythonTrampolineTargetSelector;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.loader.PythonLoaderFactory;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.python.types.TensorType;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cast.util.SourceBuffer;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.ClassTargetSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyClassTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyMethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ContextInsensitiveSelector;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationSystem;
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
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAOptions;
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

public class PythonDriver {

	private IClassHierarchy cha;
	
	private AnalysisScope scope;
	
	private CallGraph CG;
	
	private PointerAnalysis<InstanceKey> PA;
	
	private Graph<PointsToSetVariable> flowGraph;
	
	private PropagationSystem system;
	
	public PythonDriver(SourceURLModule M) throws ClassHierarchyException {
		ClassLoaderReference dialogs = PythonTypes.pythonLoader;
		
		PythonLoaderFactory loader  = new PythonLoaderFactory();
		
		scope = new AnalysisScope(Collections.singleton(PythonLanguage.Python)) { 
			{
				loadersByName.put(dialogs.getName(), dialogs);
				loadersByName.put(SYNTHETIC, new ClassLoaderReference(SYNTHETIC, PythonLanguage.Python.getName(), PythonTypes.pythonLoader));
			}
		};
		scope.addToScope(dialogs, M);
		
		cha = SeqClassHierarchyFactory.make(scope, loader);
	}

	public IClassHierarchy getClassHierarchy() {
		return cha;
	}

	private final Map<Atom,IField> fields = HashMapFactory.make();
	
	public CallGraph getCallGraph(String rootScriptName) throws IllegalArgumentException, CancelException {
		AnalysisOptions options = new AnalysisOptions();
		IRFactory<IMethod> irs = AstIRFactory.makeDefaultFactory();

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
		
		MethodTargetSelector targetSelector = new PythonTrampolineTargetSelector(new PythonConstructorTargetSelector(new ClassHierarchyMethodTargetSelector(cha)));
		targetSelector = new BypassMethodTargetSelector(targetSelector, xml.getSummaries(), xml.getIgnoredPackages(), cha);
		options.setSelector(targetSelector);
		
		ClassTargetSelector cs = new ClassHierarchyClassTargetSelector(cha);
		cs = new BypassClassTargetSelector(cs, xml.getAllocatableClasses(), cha, cha.getLoader(scope.getSyntheticLoader()));
		options.setSelector(cs);

		IClass entry = cha.lookupClass(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.findOrCreate(rootScriptName)));
		assert entry != null: "bad root name " + rootScriptName;
		MethodReference er = MethodReference.findOrCreate(entry.getReference(), AstMethodReference.fnSelector);
		options.setEntrypoints(Collections.singleton(new DefaultEntrypoint(er, cha)));

		IAnalysisCacheView cache = new AnalysisCacheImpl(irs, options.getSSAOptions());
		PythonSSAPropagationCallGraphBuilder builder = 
				new PythonSSAPropagationCallGraphBuilder(cha, options, cache, new AstCFAPointerKeys());

		AstContextInsensitiveSSAContextInterpreter interpreter = new AstContextInsensitiveSSAContextInterpreter(options, cache);
		builder.setContextInterpreter(interpreter);

		builder.setContextSelector(new ContextInsensitiveSelector());

		builder.setInstanceKeys(new PythonScopeMappingInstanceKeys(builder, new ZeroXInstanceKeys(options, cha, interpreter, ZeroXInstanceKeys.ALLOCATIONS)));

		CG = builder.makeCallGraph(options);
		PA = builder.getPointerAnalysis();
		flowGraph = builder.getPropagationSystem().getFlowGraphIncludingImplicitConstraints();
		system = builder.getPropagationSystem();
		
		return CG;
	}
	
	Graph<PointsToSetVariable> getTensorGraph() {
/*
 		TypeReference tensor = TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName("Ltensorflow/examples/tutorials/mnist/dataset"));
		Set<PointsToSetVariable> sources = HashSetFactory.make();
		flowGraph.forEach((pts) -> {
			PointerKey k = pts.getPointerKey();
			if (k instanceof LocalPointerKey) {
				CGNode n = ((LocalPointerKey)k).getNode();
				int vn = ((LocalPointerKey)k).getValueNumber();
				SSAInstruction def = n.getDU().getDef(vn);
				if (def instanceof SSANewInstruction) {
					SSANewInstruction inst = (SSANewInstruction) def;
					if (((SSANewInstruction) def).getConcreteType().equals(tensor)) {
						sources.add(pts);
					}
				}
			}
		});
		
		Atom functions = Atom.findOrCreateUnicodeAtom("tensorflow/functions");
		Set<PointsToSetVariable> sinks = HashSetFactory.make();
		flowGraph.forEach((pts) -> {
			PointerKey k = pts.getPointerKey();
			if (k instanceof LocalPointerKey) {
				CGNode n = ((LocalPointerKey)k).getNode();
				int vn = ((LocalPointerKey)k).getValueNumber();
				if (n.getMethod().getDeclaringClass().getReference().getName().getPackage().equals(functions) 
						 &&
					vn <= n.getIR().getNumberOfParameters()) 
				{
					sinks.add(pts);
				}
			}
		});
		
		return GraphSlicer.prune(flowGraph, (pts) -> {
			if (pts.getValue() != null) {
				IntIterator objects = pts.getValue().intIterator();
				while (objects.hasNext()) {
					if (PA.getInstanceKeyMapping().getMappedObject(objects.next()).getConcreteType().getReference().equals(tensor)) {
						return true;
					}
				}
			}
			return false;
		});
		*/
		return flowGraph;
	}
	
	private static final MethodReference reshape = MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName("Ltensorflow/functions/reshape")), AstMethodReference.fnSelector);
	private static final MethodReference conv2d = MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName("Ltensorflow/functions/conv2d")), AstMethodReference.fnSelector);
	
	public static void main(String args[]) throws ClassHierarchyException, IOException, IllegalArgumentException, CancelException {
		File f = new File(args[0]);
		SourceURLModule e = new SourceURLModule(f.exists()? f.toURI().toURL(): new URL(args[0]));
		PythonDriver x = new PythonDriver(e);
		IRFactory<IMethod> irs = AstIRFactory.makeDefaultFactory();
		IClassHierarchy cha = x.getClassHierarchy();
		for(IClass c : cha) {
			System.err.println(c + " : " + c.getName() + " : " + c.getSuperclass());
			for(IField ff : c.getAllStaticFields()) {
				System.err.println(ff);
			}
			for(IMethod m : c.getDeclaredMethods()) {
				System.err.println(m);
			}
		}

		for(IClass c : cha) {
			for(IMethod m : c.getDeclaredMethods()) {
				IR ir = irs.makeIR(m, Everywhere.EVERYWHERE, SSAOptions.defaultOptions());
				for(SSAInstruction inst: ir.getInstructions()) {
					if (inst != null) {
						Position p = ((AstMethod)m).getSourcePosition(inst.iindex);
						if (p !=  null) {
							System.err.println(inst + " : " + new SourceBuffer(p));
						} else {
							System.err.println(inst);							
						}
					}
				}
			}
		}

		if (args.length > 1) {
			CallGraph CG = x.getCallGraph("Lscript " + args[1]);
	
			for(CGNode n : CG) {
				System.err.println(n.getIR());
			}

			Graph<PointsToSetVariable> dataflow = SlowSparseNumberedGraph.duplicate(x.getTensorGraph());
			System.err.println(dataflow);
			System.err.println(CG);

			Set<PointsToSetVariable> sources = getDataflowSources(dataflow);
			System.err.println(sources);
			
			TensorType mnistData = TensorType.mnistInput();
			Map<PointsToSetVariable, TensorType> init = HashMapFactory.make();
			for(PointsToSetVariable v : sources) {
				init.put(v, mnistData);
			}
			
			Map<PointsToSetVariable, TensorType> reshapeTypes = x.getReshapeTypes();			
			for(PointsToSetVariable to : reshapeTypes.keySet()) {
				assert to.getPointerKey() instanceof ReturnValueKey;
				PointerKey from = x.PA.getHeapModel().getPointerKeyForLocal(((ReturnValueKey)to.getPointerKey()).getNode(), 2);
				dataflow.addEdge(x.system.findOrCreatePointsToSet(from), to);
			}
			
			TensorTypeAnalysis tt = new TensorTypeAnalysis(dataflow, init, reshapeTypes);
			
			tt.solve(new NullProgressMonitor());
			
			System.err.println(init);
			Set<PointsToSetVariable> targets1 = x.getDataflowTargets(reshape);
			for(PointsToSetVariable t : targets1) {
				System.err.println(t + ":" + tt.getIn(t));
			}
			Set<PointsToSetVariable> targets2 = x.getDataflowTargets(conv2d);
			for(PointsToSetVariable t : targets2) {
				System.err.println(t + ":" + tt.getIn(t));
			}
		}
	}

	private Map<PointsToSetVariable,TensorType> getReshapeTypes() {
		Map<PointsToSetVariable,TensorType> targets = HashMapFactory.make();
		for(CGNode n : CG) {
			if (n.getMethod().getReference().equals(reshape)) {
				for(Iterator<CGNode> srcs = CG.getPredNodes(n); srcs.hasNext(); ) {
					CGNode src = srcs.next();
					for(Iterator<CallSiteReference> sites = CG.getPossibleSites(src, n); sites.hasNext(); ) {
						CallSiteReference site = sites.next();
						for(SSAAbstractInvokeInstruction call : src.getIR().getCalls(site)) {
							targets.put(
								system.findOrCreatePointsToSet(PA.getHeapModel().getPointerKeyForReturnValue(n)),
								TensorType.reshapeArg(src, call.getUse(2)));
						}
					}
				}
			}
		}
		return targets;
	}

	private Set<PointsToSetVariable> getDataflowTargets(MethodReference reshape) {
		Set<PointsToSetVariable> targets = HashSetFactory.make();
		for(CGNode n : CG) {
			if (n.getMethod().getReference().equals(reshape)) {
				targets.add(system.findOrCreatePointsToSet(PA.getHeapModel().getPointerKeyForLocal(n, 2)));
			}
		}
		return targets;
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
}