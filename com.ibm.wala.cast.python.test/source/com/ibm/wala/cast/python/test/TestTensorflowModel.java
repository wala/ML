package com.ibm.wala.cast.python.test;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class TestTensorflowModel extends TestPythonCallGraphShape {

	@Test
	public void testTf1() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
		CallGraph CG = process("tf1.py");
		System.err.println(CG);
		
		Collection<CGNode> nodes = getNodes(CG, "Lmodel_fn");
		assert ! nodes.isEmpty() : "model_fn should be called";
		check: {
			for(CGNode node : nodes) {
				for(Iterator<CGNode> ns = CG.getPredNodes(node); ns.hasNext(); ) {
					if (ns.next().getMethod().isSynthetic()) {
						break check;
					}
				}
			
				assert false : node + " should have synthetic caller";
			}
		}
	}

}
