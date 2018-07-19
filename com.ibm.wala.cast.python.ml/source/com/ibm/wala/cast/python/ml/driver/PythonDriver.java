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
package com.ibm.wala.cast.python.ml.driver;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.lsp.WALAServer;
import com.ibm.wala.cast.python.ml.analysis.PandasReadExcelAnalysis;
import com.ibm.wala.cast.python.ml.analysis.TensorTypeAnalysis;
import com.ibm.wala.cast.python.ml.analysis.TensorVariable;
import com.ibm.wala.cast.python.ml.client.PythonTensorAnalysisEngine;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashSetFactory;

public class PythonDriver {

	private static String getTypeNameString(TypeName typ) {
		String str = typ.toString();
		if(str.startsWith("L")) {
			str = str.substring(1);
		}
		str = str.replaceAll("/", ".");
		return str;
	}
	public static final Function<WALAServer, Function<String, AbstractAnalysisEngine<InstanceKey, ? extends PropagationCallGraphBuilder, ?>>> python = (WALAServer lsp) -> {
		return (String language) -> {
			assert "python".equals(language) : language;
			PythonTensorAnalysisEngine engine = new PythonTensorAnalysisEngine() {

				@Override
				public TensorTypeAnalysis performAnalysis(
						PropagationCallGraphBuilder builder) throws CancelException {

					TensorTypeAnalysis tt = super.performAnalysis(builder);

					CallGraph CG = builder.getCallGraph();
					PointerAnalysis<InstanceKey> PA = builder.getPointerAnalysis();
					HeapModel H = PA.getHeapModel();

					CG.iterator().forEachRemaining((CGNode n) -> { 
						IMethod M = n.getMethod();
						if (M instanceof AstMethod) {
							IR ir = n.getIR();
							ir.iterateAllInstructions().forEachRemaining((SSAInstruction inst) -> {
								if (inst.iindex != -1) {
									Position pos = ((AstMethod)M).debugInfo().getInstructionPosition(inst.iindex);
									if (pos != null) {
										lsp.add(pos, new int[] {CG.getNumber(n), inst.iindex});
									}
									if (inst.hasDef()) {
										PointerKey v = H.getPointerKeyForLocal(n, inst.getDef());
										if (M instanceof AstMethod) {
											if (pos != null) {
												lsp.add(pos, v);
											}
										}
									}
								}
							});
						}
					});

					lsp.addValueAnalysis("type", builder.getPointerAnalysis().getHeapGraph(), (Boolean useMarkdown, PointerKey v) -> {
						if (builder.getPropagationSystem().isImplicit(v)) {
							return null;
						} else {
							PointsToSetVariable pts = builder.getPropagationSystem().findOrCreatePointsToSet(v);
							if (tt.getProblem().getFlowGraph().containsNode(pts)) {
								TensorVariable vv = tt.getOut(pts);
								String str = vv.toCString(useMarkdown);
								return str;
							} else {
								return null;
							}
						}
					});

					lsp.addInstructionAnalysis("target", (Boolean useMarkdown, int[] instId) -> {
						CGNode node = builder.getCallGraph().getNode(instId[0]);
						SSAInstruction[] insts = node.getIR().getInstructions();
						if (insts.length > instId[1]) {
							SSAInstruction inst = insts[instId[1]];
							if (inst instanceof SSAAbstractInvokeInstruction) {
								CallSiteReference ref = ((SSAAbstractInvokeInstruction)inst).getCallSite();
								final Set<CGNode> possibleTargets = builder.getCallGraph().getPossibleTargets(node, ref);

								if(possibleTargets.isEmpty()) {
									return null;
								}

								final String delim;
								if(useMarkdown) {
									delim = "     _or_ ";
								} else {
									delim = "     or ";
								}

								final String targetStringList = possibleTargets
										.stream()
										.map(callee ->
										getTypeNameString(callee.getMethod().getDeclaringClass().getName()))
										.distinct()
										.collect(Collectors.joining(delim));

								return targetStringList;
							}
						}
						return null;
					});	
					
					lsp.setFindDefinitionAnalysis((int[] instId) -> {
						CGNode node = builder.getCallGraph().getNode(instId[0]);
						SSAInstruction inst = node.getIR().getInstructions()[instId[1]];
						if (inst instanceof SSAAbstractInvokeInstruction) {
							CallSiteReference ref = ((SSAAbstractInvokeInstruction)inst).getCallSite();
								final Set<CGNode> possibleTargets = builder.getCallGraph().getPossibleTargets(node, ref);


							final Set<Position> targetPositions = possibleTargets
							.stream()
							.map(callee -> {
								IMethod method = callee.getMethod();
								if (method instanceof AstMethod) {
									AstMethod amethod = (AstMethod)method;
									return amethod.getSourcePosition();
								} else {
									return null;
								}
							})
							.filter(x -> x != null)
							.distinct()
							.collect(Collectors.toSet());

							return targetPositions;
						} else {
							return null;
						}
					});	
					
					lsp.addValueErrors(language, this.getErrors());
					
					Map<InstanceKey, Set<String>> excelReads = PandasReadExcelAnalysis.readExcelAnalysis(CG, PA, H);
					lsp.addValueAnalysis("columns", builder.getPointerAnalysis().getHeapGraph(), (Boolean useMarkdown, PointerKey v) -> {
						Set<String> fields = HashSetFactory.make();
						PA.getPointsToSet(v).forEach((InstanceKey o) -> {
							if (excelReads.containsKey(o)) {
								fields.addAll(excelReads.get(o));
							}
						});
						if (fields.isEmpty()) {
							return null;
						} else {
							return fields.toString();
						}
					});
					
					return tt;
				}	
			};

			return engine;
		};
	};
}
