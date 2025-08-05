/******************************************************************************
 * Copyright (c) 2018 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
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
