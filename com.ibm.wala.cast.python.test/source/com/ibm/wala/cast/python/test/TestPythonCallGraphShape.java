package com.ibm.wala.cast.python.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cast.python.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.analysis.TensorVariable;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.client.PythonTensorAnalysisEngine;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.test.TestCallGraphShape;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.strings.Atom;

public abstract class TestPythonCallGraphShape extends TestCallGraphShape {
	
	@Override
	protected Collection<CGNode> getNodes(CallGraph CG, String functionIdentifier) {
		if (functionIdentifier.contains(":")) {
			String cls = functionIdentifier.substring(0, functionIdentifier.indexOf(":"));
			String name = functionIdentifier.substring(functionIdentifier.indexOf(":")+1);
			return CG.getNodes(MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName("L" + cls)), Atom.findOrCreateUnicodeAtom(name), AstMethodReference.fnDesc));
		} else {
			return CG.getNodes(MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName("L" + functionIdentifier)), AstMethodReference.fnSelector));
		}
	}

	protected SourceURLModule getScript(String name) throws IOException {
		try {
			URL url = new URL(name);
			return new SourceURLModule(url);
		} catch (MalformedURLException e) {
			return new SourceURLModule(getClass().getClassLoader().getResource(name));
		}
	}
	
	protected CallGraph process(String name) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine engine = new PythonTensorAnalysisEngine();
		engine.setModuleFiles(Collections.singleton(getScript(name)));
		return engine.buildDefaultCallGraph();
	}
	
	StringBuffer dump(CallGraph CG) {
		StringBuffer sb = new StringBuffer();
		for(CGNode n : CG) {
			sb.append(n.getIR()).append("\n");
		}
		return sb;
	}

	protected void checkReshape(PropagationCallGraphBuilder cgBuilder, CallGraph CG, TensorTypeAnalysis result, String in, String out) {
		boolean found = false;
		Set<CGNode> reshapes = CG.getNodes(MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, "Ltensorflow/functions/reshape"), AstMethodReference.fnSelector));
		assert reshapes.size() > 0;
		for(CGNode reshape : reshapes) {
			for(Iterator<CGNode> callers = CG.getPredNodes(reshape); callers.hasNext(); ) {
				CGNode caller = callers.next();
				for(Iterator<CallSiteReference> sites = CG.getPossibleSites(caller, reshape); sites.hasNext(); ) {
					for(SSAAbstractInvokeInstruction call : caller.getIR().getCalls(sites.next())) {
						TensorVariable orig = result.getOut(cgBuilder.getPropagationSystem().findOrCreatePointsToSet(cgBuilder.getPointerKeyForLocal(caller, call.getUse(1))));
						boolean thisOne = in.equals(orig.getTypes().toString());
						
						TensorVariable reshaped = result.getOut(cgBuilder.getPropagationSystem().findOrCreatePointsToSet(cgBuilder.getPointerKeyForLocal(caller, call.getDef())));
						thisOne &= out.equals(reshaped.getTypes().toString());
						
						if (thisOne) {
							found = true;
							break;
						}
					}
				}
			}
		}
		assert found;
	}
}
