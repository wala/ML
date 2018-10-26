package com.ibm.wala.cast.python.test;

import java.io.IOException;
import java.util.Set;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.client.PythonTurtleAnalysisEngine;
import com.ibm.wala.cast.python.client.PythonTurtleAnalysisEngine.TurtlePath;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;

public class TestPythonTurtleCallGraphShape extends TestPythonCallGraphShape {

	@Override
	protected PythonAnalysisEngine<Set<TurtlePath>> makeEngine(String... name) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<Set<TurtlePath>> engine = new PythonTurtleAnalysisEngine();
		Set<Module> modules = HashSetFactory.make();
		for(String n : name) {
			modules.add(getScript(n));
		}
		engine.setModuleFiles(modules);
		return engine;
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		TestPythonCallGraphShape driver = new TestPythonTurtleCallGraphShape() {
			
		};
		
		PythonAnalysisEngine<?> E = driver.makeEngine(args[0]);
		
		CallGraphBuilder<? super InstanceKey> builder = E.defaultCallGraphBuilder();
		CallGraph CG = builder.makeCallGraph(E.getOptions(), new NullProgressMonitor());
		
		for(TurtlePath p : (Set<TurtlePath>)E.performAnalysis((SSAPropagationCallGraphBuilder)builder)) {
			System.err.println(p);
		}
		
		CAstCallGraphUtil.AVOID_DUMP = false;
		CAstCallGraphUtil.dumpCG(((SSAPropagationCallGraphBuilder)builder).getCFAContextInterpreter(), E.getPointerAnalysis(), CG);
	}

}
