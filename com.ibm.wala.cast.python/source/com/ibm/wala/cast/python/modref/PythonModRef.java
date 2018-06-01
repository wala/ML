package com.ibm.wala.cast.python.modref;
import java.util.Collection;

import com.ibm.wala.cast.ipa.callgraph.AstHeapModel;
import com.ibm.wala.cast.ipa.modref.AstModRef;
import com.ibm.wala.cast.python.ssa.PythonInstructionVisitor;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;

public class PythonModRef extends AstModRef<InstanceKey> {

	public static class PythonRefVisitor<T extends InstanceKey> extends AstRefVisitor<T> implements PythonInstructionVisitor {

		public PythonRefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis<T> pa, ExtendedHeapModel h) {
			super(n, result, pa, (AstHeapModel) h);
		}
		
	}
	
	@Override
	protected RefVisitor<InstanceKey, ? extends ExtendedHeapModel> makeRefVisitor(CGNode n,
			Collection<PointerKey> result, PointerAnalysis<InstanceKey> pa, ExtendedHeapModel h) {
		return new PythonRefVisitor<>(n, result, pa, h);
	}

	public static class PythonModVisitor<T extends InstanceKey> extends AstModVisitor<T> implements PythonInstructionVisitor {

		public PythonModVisitor(CGNode n, Collection<PointerKey> result, ExtendedHeapModel h,
				PointerAnalysis<T> pa, boolean ignoreAllocHeapDefs) {
			super(n, result, (AstHeapModel) h, pa);
		}
		
	}
	
	@Override
	protected ModVisitor<InstanceKey, ? extends ExtendedHeapModel> makeModVisitor(CGNode n,
			Collection<PointerKey> result, PointerAnalysis<InstanceKey> pa, ExtendedHeapModel h,
			boolean ignoreAllocHeapDefs) {
		return new PythonModVisitor<>(n, result, h, pa, ignoreAllocHeapDefs);
	}

}
