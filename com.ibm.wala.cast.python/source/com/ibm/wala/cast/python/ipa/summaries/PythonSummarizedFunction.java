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
package com.ibm.wala.cast.python.ipa.summaries;

import com.ibm.wala.cast.python.cfg.PythonInducedCFG;
import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethodWithNames;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;

public class PythonSummarizedFunction extends SummarizedMethodWithNames {

	public PythonSummarizedFunction(MethodReference ref, MethodSummary summary, IClass declaringClass)
			throws NullPointerException {
		super(ref, summary, declaringClass);
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public InducedCFG makeControlFlowGraph(SSAInstruction[] instructions) {
		return new PythonInducedCFG(instructions, this, Everywhere.EVERYWHERE);	
	}

}
