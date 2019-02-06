package com.ibm.wala.cast.python.ssa;

import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;

public class PythonImportFromGetInstruction extends SSAGetInstruction {
    public PythonImportFromGetInstruction(int iindex, int result, int ref, FieldReference field){
        super(iindex, result, ref, field);
    }

    public PythonImportFromGetInstruction(int iindex, int result, FieldReference field){
        super(iindex, result, field);
    }

    public String toString(SymbolTable symbolTable){
        return getValueString(symbolTable, getDef()) + " = importFromGet " + getDeclaredField() + " "
                + getValueString(symbolTable, getRef());
    }
}
