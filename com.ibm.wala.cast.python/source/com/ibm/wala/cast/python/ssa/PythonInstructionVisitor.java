package com.ibm.wala.cast.python.ssa;

import com.ibm.wala.cast.ir.ssa.AstInstructionVisitor;

public interface PythonInstructionVisitor extends AstInstructionVisitor {
 
	default void visitPythonInvoke(PythonInvokeInstruction inst) {
		
	}
	
}
