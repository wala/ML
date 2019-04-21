package com.ibm.wala.cast.python.test;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;

import org.junit.Test;

import com.ibm.wala.cast.ipa.callgraph.CAstCallGraphUtil;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
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
		
		CGNode node = nodes.iterator().next();
		DefUse du = node.getDU();
		IR script = node.getIR();

		SSAAbstractInvokeInstruction sliceImport = find(script, (SSAAbstractInvokeInstruction inst) -> {
			if (inst.getNumberOfUses() > 0) {
				int f = inst.getUse(0);
				SSAInstruction def = du.getDef(f);
				if (def instanceof SSANewInstruction) {
					return "Lwala/builtin/slice".equals(((SSANewInstruction)def).getConcreteType().getName().toString());				
				} 
			}
			
			return false;
			
		});
		
		assert sliceImport != null;		
	}	
	
	 protected static final Object[][] assertionsSlice2 = new Object[][] {
		    new Object[] { ROOT, new String[] { "script slice2.py" } },
		    new Object[] {
		        "script slice2.py",
		        new String[] { "wala/builtin/slice", "script slice2.py/a", "script slice2.py/b", "script slice2.py/c", "script slice2.py/d" } }
	 };

	@Test
	public void testSlice2() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<?> engine = makeEngine("slice2.py");
		SSAPropagationCallGraphBuilder builder = (SSAPropagationCallGraphBuilder) engine.defaultCallGraphBuilder();
		CallGraph CG = builder.makeCallGraph(builder.getOptions());
		CAstCallGraphUtil.AVOID_DUMP = false;
		CAstCallGraphUtil.dumpCG((SSAContextInterpreter)builder.getContextInterpreter(), builder.getPointerAnalysis(), CG);
		verifyGraphAssertions(CG, assertionsSlice2);

		Collection<CGNode> nodes = getNodes(CG, "script slice2.py");
		assert nodes.size() == 1;
	}
}
