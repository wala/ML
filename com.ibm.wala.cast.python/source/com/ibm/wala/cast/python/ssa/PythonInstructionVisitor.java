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
package com.ibm.wala.cast.python.ssa;

import com.ibm.wala.cast.ir.ssa.AstInstructionVisitor;

public interface PythonInstructionVisitor extends AstInstructionVisitor {

  default void visitPythonInvoke(PythonInvokeInstruction inst) {}

  default void visitForElementGet(ForElementGetInstruction forElementGet) {}
}
