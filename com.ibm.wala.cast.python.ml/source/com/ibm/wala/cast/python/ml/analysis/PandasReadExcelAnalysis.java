package com.ibm.wala.cast.python.ml.analysis;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.ir.ssa.AstPropertyRead;
import com.ibm.wala.cast.python.ssa.PythonInstructionVisitor;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;

public class PandasReadExcelAnalysis {

	public static Map<InstanceKey, Set<String>> readExcelAnalysis(CallGraph CG, PointerAnalysis<? extends InstanceKey> PA,
			HeapModel H) {
		Set<CGNode> read_excel = CG.getNodes(MethodReference.findOrCreate(TypeReference.findOrCreate(PythonTypes.pythonLoader, "Lpandas/functions/read_excel"), AstMethodReference.fnSelector));

		Map<InstanceKey, Set<String>> excelTableFields = HashMapFactory.make();

		for(CGNode script : CG) {
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
				public void visitPropertyRead(AstPropertyRead instruction) {
					PA.getPointsToSet(H.getPointerKeyForLocal(script, instruction.getObjectRef())).forEach((InstanceKey obj) -> {
						if (excelTableFields.containsKey(obj)) {
							if (excelTableFields.containsKey(obj)) {
								PA.getPointsToSet(H.getPointerKeyForLocal(script, instruction.getMemberRef())).forEach((InstanceKey field) -> {
									if (field instanceof ConstantKey<?> && 
										((ConstantKey<?>)field).getValue() instanceof String)
									{
										excelTableFields.get(obj).add(((ConstantKey<String>)field).getValue());
									}
								});
							}
						}
					});
				}

				@Override
				public void visitGet(SSAGetInstruction instruction) {
					PA.getPointsToSet(H.getPointerKeyForLocal(script, instruction.getRef())).forEach((InstanceKey obj) -> {
						if (excelTableFields.containsKey(obj)) {
							excelTableFields.get(obj).add(instruction.getDeclaredField().getName().toString());
						}
					});
				}
			});
		}
		return excelTableFields;
	}

}
