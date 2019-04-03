package com.ibm.wala.cast.python.ssa;

import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;

public class PythonStoreProperty extends SSAArrayStoreInstruction {

	public PythonStoreProperty(int iindex, int objectRef, int memberRef, int value) {
		super(iindex, objectRef, memberRef, value, PythonTypes.Root);
	}

	@Override
	public void visit(IVisitor v) {
		((PythonInstructionVisitor)v).visitPythonStoreProperty(this);
	}

}
