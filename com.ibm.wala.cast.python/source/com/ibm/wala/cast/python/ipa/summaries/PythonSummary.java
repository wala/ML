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
package com.ibm.wala.cast.python.ipa.summaries;

import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

public class PythonSummary extends MethodSummary {

  private final int declaredParameters;

  public PythonSummary(MethodReference ref, int declaredParameters) {
    super(ref);
    this.declaredParameters = declaredParameters;
  }

  @Override
  public int getNumberOfParameters() {
    return declaredParameters;
  }

  @Override
  public TypeReference getParameterType(int i) {
    return PythonTypes.Root;
  }

}

