package com.ibm.wala.cast.python.ml.test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cast.python.ml.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.ssa.PythonInstructionVisitor;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;

import edu.emory.mathcs.backport.java.util.Collections;

public class TestPandasModel extends TestPythonMLCallGraphShape {

	@Test
	public void testPandas1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<TensorTypeAnalysis> engine = builder("pandas1.py");
		CallGraphBuilder<? extends InstanceKey> builder = engine.defaultCallGraphBuilder();
		CallGraph CG = builder.makeCallGraph(engine.getOptions(), new NullProgressMonitor());
		PointerAnalysis<? extends InstanceKey> PA = engine.getPointerAnalysis();
		HeapModel H = PA.getHeapModel();
		
		CGNode script = getNodes(CG, "script pandas1.py").iterator().next();
		Set<CGNode> read_excel = CG.getNodes(MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, "Lpandas/functions/read_excel"), AstMethodReference.fnSelector));
		
		Map<InstanceKey, Set<String>> excelTableFields = HashMapFactory.make();
		
		IR ir = script.getIR();		
		ir.visitAllInstructions(new PythonInstructionVisitor() {
			@Override
			public void visitPythonInvoke(PythonInvokeInstruction inst) {
				if (! Collections.disjoint(CG.getPossibleTargets(script, inst.getCallSite()), read_excel)) {
					PA.getPointsToSet(H.getPointerKeyForLocal(script, inst.getDef())).forEach((InstanceKey obj) -> {
						excelTableFields.put(obj, HashSetFactory.make());
					});
				}
			} 	
		});
		ir.visitAllInstructions(new PythonInstructionVisitor() {
			@Override
			public void visitGet(SSAGetInstruction instruction) {
				PA.getPointsToSet(H.getPointerKeyForLocal(script, instruction.getRef())).forEach((InstanceKey obj) -> {
					if (excelTableFields.containsKey(obj)) {
						excelTableFields.get(obj).add(instruction.getDeclaredField().getName().toString());
					}
				});
			}
		});

		System.out.println(excelTableFields);
	
		boolean foundDfqol = false;
		boolean foundDfdemog = false;
		for (Map.Entry<InstanceKey, Set<String>> e : excelTableFields.entrySet()) {
			if (e.getValue().contains("PID") && !e.getValue().contains("Patient_ID")) {
				foundDfqol = true;
			} else if (!e.getValue().contains("PID") && e.getValue().contains("Patient_ID")) {
				foundDfdemog = true;
			}
		}
		
		assert foundDfqol && foundDfdemog;
	}

}
