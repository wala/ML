package com.ibm.wala.cast.python.test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.client.PythonTurtleAnalysisEngine;
import com.ibm.wala.cast.python.client.PythonTurtleAnalysisEngine.TurtlePath;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationSystem;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.traverse.DFS;

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
		for(CGNode n : CG) {
			System.err.println(n.getIR());
		}
		System.err.println(CG);
		System.err.println(stuff);
		
		PointerAnalysis<? super InstanceKey> PA = builder.getPointerAnalysis();
		PropagationSystem system = ((SSAPropagationCallGraphBuilder)builder).getPropagationSystem();
		Graph<PointsToSetVariable> ptr_G = system.getFlowGraphIncludingImplicitConstraints();
		for(Object elt : stuff) {
			JSONObject e  = (JSONObject)elt;
			if ("[\"merge\",\"pandas\"]".equals(String.valueOf(e.get("path")))) {
				System.err.println(e);
				CGNode cgnode = CG.getNode(e.getInt("node"));
				IR ir = cgnode.getIR();
				System.err.println(ir);
				SSAInstruction reduce = cgnode.getDU().getDef(e.getInt("vn"));
				PA.getPointsToSet(PA.getHeapModel().getPointerKeyForLocal(cgnode, reduce.getUse(1))).forEach((ik) -> {
					((InstanceKey)ik).getCreationSites(CG).forEachRemaining((cs) -> {
						PointerKey startKey = PA.getHeapModel().getPointerKeyForReturnValue(cs.fst);
						PointsToSetVariable startV = system.findOrCreatePointsToSet(startKey);
						DFS.getReachableNodes(ptr_G, Collections.singleton(startV)).forEach((val) -> {
							PointerKey vk = val.getPointerKey();
							if (vk instanceof LocalPointerKey) {
								LocalPointerKey lpk = (LocalPointerKey) vk;
								lpk.getNode().getDU().getUses(lpk.getValueNumber()).forEachRemaining((use) -> {
									System.err.println(use);
								});
							}
						});
					});
				});
			}
		}
	}

}
