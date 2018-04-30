/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.python.ssa;

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.cast.ir.ssa.AstPropertyRead;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.types.TypeReference;

public class PythonPropertyRead extends AstPropertyRead {
  public PythonPropertyRead(int iindex, int result, int objectRef, int memberRef) {
    super(iindex, result, objectRef, memberRef);
  }

  /* (non-Javadoc)
   * @see com.ibm.domo.ssa.Instruction#getExceptionTypes()
   */
  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return Collections.singleton(PythonTypes.Root);
  }
}
