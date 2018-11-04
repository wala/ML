package com.ibm.wala.cast.python.test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

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
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.Graph;

public class TestPythonTurtleCallGraphShape extends TestPythonCallGraphShape {

	@Override
	protected PythonAnalysisEngine<Graph<TurtlePath>> makeEngine(String... name) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<Graph<TurtlePath>> engine = new PythonTurtleAnalysisEngine();
		Set<Module> modules = HashSetFactory.make();
		for(String n : name) {
			modules.add(getScript(n));
		}
		engine.setModuleFiles(modules);
		return engine;
	}

	public static void main(String[] args) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		TestPythonTurtleCallGraphShape driver = new TestPythonTurtleCallGraphShape() {
			
		};
		
		PythonAnalysisEngine<Graph<TurtlePath>> E = driver.makeEngine(args[0]);

		CallGraphBuilder<? super InstanceKey> builder = E.defaultCallGraphBuilder();
		CallGraph CG = builder.makeCallGraph(E.getOptions(), new NullProgressMonitor());
		
		Graph<TurtlePath> analysis = E.performAnalysis((SSAPropagationCallGraphBuilder)builder);
		
		int I = 0;
		Map<TurtlePath,Integer> idx = HashMapFactory.make();
		JSONArray stuff = new JSONArray();
		for(TurtlePath tp : analysis) {
			stuff.put(tp.toJSON());
			idx.put(tp, I++);
		}
		analysis.forEach((TurtlePath src) -> {
			JSONArray succ = new JSONArray();
			analysis.getSuccNodes(src).forEachRemaining((TurtlePath dst) -> {
				succ.put(idx.get(dst));
			});
			((JSONObject)stuff.get(idx.get(src))).put("edges", succ);
		});
		
		System.err.println(stuff);
		
		CAstCallGraphUtil.AVOID_DUMP = false;
		CAstCallGraphUtil.dumpCG(((SSAPropagationCallGraphBuilder)builder).getCFAContextInterpreter(), E.getPointerAnalysis(), CG);
	}

}
