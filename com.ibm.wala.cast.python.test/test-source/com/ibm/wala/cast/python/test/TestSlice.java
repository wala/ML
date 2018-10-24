package com.ibm.wala.cast.python.test;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;

import org.junit.Test;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;

public class TestSlice extends TestPythonCallGraphShape {

	private static SSAAbstractInvokeInstruction find(IR ir, Function<SSAAbstractInvokeInstruction,Boolean> filter) {
		for(SSAInstruction inst : ir.getInstructions()) {
			if (inst instanceof SSAAbstractInvokeInstruction && filter.apply((SSAAbstractInvokeInstruction)inst)) {
				return (SSAAbstractInvokeInstruction)inst;
			}
		}
		
		return null;
	}
	
	@Test
	public void testSlice1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("slice1.py");
		
		Collection<CGNode> nodes = getNodes(CG, "script slice1.py");
		assert nodes.size() == 1;
		
		IR script = nodes.iterator().next().getIR();

		SSAAbstractInvokeInstruction sliceImport = find(script, (SSAAbstractInvokeInstruction inst) -> {
			return "Lslice".equals(inst.getDeclaredTarget().getDeclaringClass().getName().toString());
		});
		
		assert sliceImport != null;
		
		int sliceVn = sliceImport.getDef();
		
		assert null != find(script, (SSAAbstractInvokeInstruction inst) -> {
			return inst.getNumberOfUses() > 0 && inst.getUse(0) == sliceVn && inst.getNumberOfPositionalParameters() == 5;
		});

		assert null != find(script, (SSAAbstractInvokeInstruction inst) -> {
			return inst.getNumberOfUses() > 0 && inst.getUse(0) == sliceVn && inst.getNumberOfPositionalParameters() == 6;
		});
	}
	
}
