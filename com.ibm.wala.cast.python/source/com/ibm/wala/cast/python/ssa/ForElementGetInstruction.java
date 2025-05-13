package com.ibm.wala.cast.python.ssa;

import com.ibm.wala.ssa.SSAAbstractBinaryInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;

public class ForElementGetInstruction extends SSAAbstractBinaryInstruction {

  public ForElementGetInstruction(int iindex, int result, int val1, int val2) {
    super(iindex, result, val1, val2);
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    if (uses == null) {
      if (defs == null) {
        return new ForElementGetInstruction(iIndex(), result, val1, val2);
      } else {
        return new ForElementGetInstruction(iIndex(), defs[0], val1, val2);
      }
    } else {
      if (defs == null) {
        return new ForElementGetInstruction(iIndex(), result, uses[0], uses[1]);
      } else {
        return new ForElementGetInstruction(iIndex(), defs[0], uses[0], uses[1]);
      }
    }
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, result)
        + " = nextElt "
        + getValueString(symbolTable, val1)
        + ", "
        + getValueString(symbolTable, val1);
  }

  @Override
  public void visit(IVisitor v) {
    ((PythonInstructionVisitor) v).visitForElementGet(this);
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }
}
