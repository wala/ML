package com.ibm.wala.cast.python.jython3.test;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.loader.CAstAbstractModuleLoader.DynamicCodeBody;
import com.ibm.wala.cast.python.client.PytestAnalysisEngine;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ipa.callgraph.PythonSSAPropagationCallGraphBuilder;
import com.ibm.wala.cast.python.loader.PythonLoader.DynamicMethodBody;
import com.ibm.wala.cast.python.modref.PythonModRef;
import com.ibm.wala.cast.python.test.TestPythonCallGraphShape;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
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
	
	private MethodReference parametrize = MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, "Lpytest/class/parametrize"), AstMethodReference.fnSelector);
			
	private static boolean isTestName(IClass obj, String prefix) {
		String name = obj.getName().toString();
		return name.contains("/") && name.substring(name.lastIndexOf('/')+1).startsWith(prefix);
	}

	private static boolean isTestMethod(IClass methodObj) {
		return isTestName(methodObj, "test_") &&
			(!(methodObj instanceof DynamicMethodBody) ||
			 isTestClass(((DynamicMethodBody)methodObj).getContainer()));
	}

	private static boolean isTestClass(IClass classObj) {
		return isTestName(classObj, "Test");
	}

	@Test
	public void testAnnotation2() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<?> bb = makeEngine(new PytestAnalysisEngine<Void>(), "annotations2.py", "pandas_shim.py", "np_shim.py");

		PythonSSAPropagationCallGraphBuilder builder = bb.defaultCallGraphBuilder();
		CallGraph CG = builder.makeCallGraph(builder.getOptions());
		
		CG.getClassHierarchy().forEach(x ->  { 
			if (x instanceof DynamicMethodBody) {
				DynamicMethodBody method = (DynamicMethodBody)x;
				if (isTestClass(method.getContainer())) {
					if (isTestMethod(method)) {
						System.err.println(x + " : " + x.getClass());						
					}
				}
			} else if (x instanceof DynamicCodeBody) {
				if (isTestMethod(x)) {
					System.err.println(x + " : " + x.getClass());
				}
			}
		});

		CG.getNodes(parametrize).forEach(p -> {
			CG.getPredNodes(p).forEachRemaining(n -> { 
				CG.getPossibleSites(n, p).forEachRemaining(s -> { 
					for (SSAAbstractInvokeInstruction inst : n.getIR().getCalls(s)) {
						System.err.println(inst);
						for(int i = 0; i < inst.getNumberOfUses(); i++) {
							PointerKey param = builder.getPointerAnalysis().getHeapModel().getPointerKeyForLocal(n, inst.getUse(i));
							System.err.println(builder.getPointerAnalysis().getPointsToSet(param));
						}
					}
				});
			});
		});
	
		CAstCallGraphUtil.AVOID_DUMP = false;
		CAstCallGraphUtil.dumpCG((SSAContextInterpreter)builder.getContextInterpreter(), builder.getPointerAnalysis(), CG);

		System.err.println(CG);
	}

}
