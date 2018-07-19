package com.ibm.wala.cast.python.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Set;

import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.test.TestCallGraphShape;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;
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
	
	protected CallGraph process(String... name) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<?> engine = new PythonAnalysisEngine<Void>() {
			@Override
			public Void performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
				assert false;
				return null;
			}
		};
		
		Set<Module> modules = HashSetFactory.make();
		for(String n : name) {
			modules.add(getScript(n));
		}
		engine.setModuleFiles(modules);
		
		return engine.buildDefaultCallGraph();
	}
	
	StringBuffer dump(CallGraph CG) {
		StringBuffer sb = new StringBuffer();
		for(CGNode n : CG) {
			sb.append(n.getIR()).append("\n");
		}
		return sb;
	}

	}
