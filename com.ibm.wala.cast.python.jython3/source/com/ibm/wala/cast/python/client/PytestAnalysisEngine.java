package com.ibm.wala.cast.python.client;

import com.ibm.wala.cast.python.ipa.callgraph.PythonSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.python.loader.PytestLoader;
import com.ibm.wala.cast.python.loader.PytestLoaderFactory;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.core.util.strings.Atom;

public class PytestAnalysisEngine<T> extends PythonAnalysisEngine<T> {

	private PythonSSAPropagationCallGraphBuilder builder;
	
	private class PytestTargetSelector implements MethodTargetSelector {
		private final MethodTargetSelector base;
				
		private PytestTargetSelector(MethodTargetSelector base) {
			this.base = base;
		}

		@Override
		public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
			if (site.getDeclaredTarget().getDeclaringClass().equals(PytestLoader.pytestType)) {
				System.err.println("pytest call site " + site + " " + receiver);
				PointerKeyFactory pkf = builder.getPointerKeyFactory();
				for(SSAAbstractInvokeInstruction inst : caller.getIR().getCalls(site)) {
					PointerKey test = pkf.getPointerKeyForLocal(caller, inst.getUse(0));
					OrdinalSet<InstanceKey> testObjs = builder.getPointerAnalysis().getPointsToSet(test);
					testObjs.forEach(obj -> { 
						IField x = obj.getConcreteType().getField(Atom.findOrCreateUnicodeAtom("params"));
						if (x != null) {
							PointerKey ps = pkf.getPointerKeyForInstanceField(obj, x);
							builder.getPointerAnalysis().getPointsToSet(ps).forEach(p -> { 
								IField ns = p.getConcreteType().getField(Atom.findOrCreateUnicodeAtom("params"));
								PointerKey names = pkf.getPointerKeyForInstanceField(p, ns);
								OrdinalSet<InstanceKey> namesObjs = builder.getPointerAnalysis().getPointsToSet(names);
								System.err.println("names: " + namesObjs);

								IField vs = p.getConcreteType().getField(Atom.findOrCreateUnicodeAtom("values"));
								PointerKey values = pkf.getPointerKeyForInstanceField(p, vs);
								OrdinalSet<InstanceKey> valsObjs = builder.getPointerAnalysis().getPointsToSet(values);
								System.err.println("values: " + valsObjs);
							});
						}
					});
					System.err.println(test + " " + testObjs);
				}
			}
			return base.getCalleeTarget(caller, site, receiver);
		}
		
	}
	
	public PytestAnalysisEngine() {
		super();
		loader = new PytestLoaderFactory();
	}
	
	protected void addBypassLogic(IClassHierarchy cha, AnalysisOptions options) {
		super.addBypassLogic(cha, options);
		
		options.setSelector(new PytestTargetSelector(options.getMethodTargetSelector()));
		
		addSummaryBypassLogic(options, "pytest.xml");
	}
	
	
	@Override
	protected PythonSSAPropagationCallGraphBuilder getCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options,
			IAnalysisCacheView cache) {
		return builder = super.getCallGraphBuilder(cha, options, cache);
	}

	@Override
	public T performAnalysis(PropagationCallGraphBuilder arg0) throws CancelException {
		return null;
	}

}
