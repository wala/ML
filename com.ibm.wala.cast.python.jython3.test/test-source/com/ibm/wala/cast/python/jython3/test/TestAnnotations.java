package com.ibm.wala.cast.python.jython3.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.ipa.callgraph.PythonSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.python.modref.PythonModRef;
import com.ibm.wala.cast.python.test.TestPythonCallGraphShape;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.util.CancelException;

public class TestAnnotations extends TestPythonCallGraphShape {

	@Test
	public void testAnnotation1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		AbstractAnalysisEngine<InstanceKey, PythonSSAPropagationCallGraphBuilder, ?> bb = makeEngine("annotations1.py");
		PythonSSAPropagationCallGraphBuilder builder = bb.defaultCallGraphBuilder();
		CallGraph CG = builder.makeCallGraph(builder.getOptions());
		
		CAstCallGraphUtil.AVOID_DUMP = false;
		CAstCallGraphUtil.dumpCG((SSAContextInterpreter)builder.getContextInterpreter(), builder.getPointerAnalysis(), CG);

		@SuppressWarnings("unchecked")
		PointerAnalysis<InstanceKey> ptr = (PointerAnalysis<InstanceKey>)bb.getPointerAnalysis();
		DataDependenceOptions data = DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS;
		ControlDependenceOptions control = ControlDependenceOptions.NONE;
		SDG<InstanceKey> sdg = new SDG<InstanceKey>(CG, ptr, new PythonModRef(), data, control);
		 
		System.err.println(sdg);
	}
}
