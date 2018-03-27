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
package com.ibm.wala.cast.python;

import java.io.IOException;
import java.util.function.Function;

import com.ibm.wala.cast.lsp.WALAServer;
import com.ibm.wala.cast.python.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.client.PythonAnalysisEngine;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;

public class PythonDriver {

	public static void main(String args[]) throws ClassHierarchyException, IOException, IllegalArgumentException, CancelException {
		Function<WALAServer, Function<String, AbstractAnalysisEngine<InstanceKey, ? extends PropagationCallGraphBuilder, ?>>> python = (WALAServer lsp) -> {
			return (String url) -> {
				PythonAnalysisEngine engine = new PythonAnalysisEngine() {

					@Override
					public TensorTypeAnalysis performAnalysis(
							PropagationCallGraphBuilder builder) throws CancelException {
						TensorTypeAnalysis tt = super.performAnalysis(builder);

						lsp.addValueAnalysis((PointerKey v) -> {
							if (builder.getPropagationSystem().isImplicit(v)) {
								return null;
							} else {
								PointsToSetVariable pts = builder.getPropagationSystem().findOrCreatePointsToSet(v);
								return tt.getOut(pts).toString();
							}
						});

						lsp.addInstructionAnalysis((int[] instId) -> {
							CGNode node = builder.getCallGraph().getNode(instId[0]);
							SSAInstruction inst = node.getIR().getInstructions()[instId[1]];
							if (inst instanceof SSAAbstractInvokeInstruction) {
								CallSiteReference ref = ((SSAAbstractInvokeInstruction)inst).getCallSite();
								String targets = "targets[ ";
								for(CGNode callee : builder.getCallGraph().getPossibleTargets(node, ref)) {
									targets += callee.getMethod().getDeclaringClass().getName().toString() + " ";
								}
								targets += "]";
								return targets;
							} else {
								return null;
							}
						});		
						return tt;
					}	
				};

				return engine;
			};
		};

		WALAServer.launch(6660, python);
	}	
}