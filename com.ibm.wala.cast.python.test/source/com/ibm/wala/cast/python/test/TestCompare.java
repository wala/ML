package com.ibm.wala.cast.python.test;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import org.junit.Test;

import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.cfg.cdg.ControlDependenceGraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.util.CancelException;

public class TestCompare extends TestPythonCallGraphShape {

	 protected static final Object[][] assertionsCmp1 = new Object[][] {
		    new Object[] { ROOT, new String[] { "script cmp1.py" } },
		    new Object[] {
		        "script cmp1.py",
		        new String[] { "script cmp1.py/ctwo", "script cmp1.py/cthree", "script cmp1.py/cfour" } }
	 };

	 private void findReturns(CGNode n, Consumer<SSAReturnInstruction> act) {
		 n.getIR().iterateNormalInstructions().forEachRemaining(inst -> { 
			 if (inst instanceof SSAReturnInstruction) {
				 act.accept((SSAReturnInstruction)inst);
			 }
		 });
	 }
	 
	@Test
	public void testAssign1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		PythonAnalysisEngine<?> e = new PythonAnalysisEngine<Void>() {
			@Override
			public Void performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
				assert false;
				return null;
			}
		};
		e.setModuleFiles(Collections.singleton(getScript("cmp1.py")));
		PropagationCallGraphBuilder cgBuilder = (PropagationCallGraphBuilder) e.defaultCallGraphBuilder();
		CallGraph CG = cgBuilder.makeCallGraph(cgBuilder.getOptions());
		verifyGraphAssertions(CG, assertionsCmp1);
		
		
		Collection<CGNode> ctwo = this.getNodes(CG, "script cmp1.py/ctwo");
		assert ! ctwo.isEmpty();
		check(ctwo);
		
		Collection<CGNode> cthree = this.getNodes(CG, "script cmp1.py/cthree");
		assert ! cthree.isEmpty();
		check(cthree);

		Collection<CGNode> cfour = this.getNodes(CG, "script cmp1.py/cfour");
		assert ! cfour.isEmpty();
		check(cfour);

	}

	private void check(Collection<CGNode> ctwo) {
		ctwo.forEach(n -> { 
			findReturns(n, inst -> {
				SSACFG cfg = n.getIR().getControlFlowGraph();
				ControlDependenceGraph<ISSABasicBlock> cdg = new ControlDependenceGraph<>(cfg, true);
				
				ISSABasicBlock bb = cfg.getBlockForInstruction(inst.iIndex());
				while(cdg.getPredNodeCount(bb) > 0) {
					ISSABasicBlock pb = cdg.getPredNodes(bb).next();
					System.err.println(bb);
					bb = pb;
				}
			});
		});
	}

}
