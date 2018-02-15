package com.ibm.wala.cast.python.test;

import java.io.IOException;
import java.util.Collection;

import com.ibm.wala.cast.python.PythonDriver;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.test.TestCallGraphShape;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;

public abstract class TestPythonCallGraphShape extends TestCallGraphShape {

	@Override
	protected Collection<CGNode> getNodes(CallGraph CG, String functionIdentifier) {
		return CG.getNodes(MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, TypeName.string2TypeName(functionIdentifier)), AstMethodReference.fnSelector));
	}

	protected SourceURLModule getScript(String name) throws IOException {
		return new SourceURLModule(getClass().getClassLoader().getResource(name));
	}
	
	protected CallGraph process(String name) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonDriver engine = new PythonDriver(getScript(name));
		return engine.getCallGraph("Lscript " + name);
	}
	
}
