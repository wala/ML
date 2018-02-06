package com.ibm.wala.cast.python.cfg;

import com.ibm.wala.cast.ir.cfg.AstInducedCFG;
import com.ibm.wala.cast.python.ssa.PythonInstructionVisitor;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ssa.SSAInstruction;

public class PythonInducedCFG extends AstInducedCFG {

	public class PythonPEIVisitor extends AstPEIVisitor implements PythonInstructionVisitor {

		public PythonPEIVisitor(boolean[] r) {
			super(r);
		}

		@Override
		public void visitPythonInvoke(PythonInvokeInstruction inst) {
			breakBasicBlock();
		}

	}

	public class PythonBranchVisitor extends AstBranchVisitor implements PythonInstructionVisitor {

		public PythonBranchVisitor(boolean[] r) {
			super(r);
		}

	}

	public PythonInducedCFG(SSAInstruction[] instructions, IMethod method, Context context) {
		super(instructions, method, context);
	}

	
	@Override
	protected BranchVisitor makeBranchVisitor(boolean[] r) {
		return new PythonBranchVisitor(r);
	}

	@Override
	protected PEIVisitor makePEIVisitor(boolean[] r) {
		return new PythonPEIVisitor(r);
	}

}
