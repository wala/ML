package com.ibm.wala.cast.python.client;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.python.ipa.summaries.TurtleSummary;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.ssa.PythonPropertyWrite;
import com.ibm.wala.cast.python.util.PythonInterpreter;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.util.SourceBuffer;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointsToSetVariable;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cfg.ExplodedInterproceduralCFG;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Iterator2List;
import com.ibm.wala.util.collections.Iterator2Set;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.graph.AbstractGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.NodeManager;
import com.ibm.wala.util.graph.labeled.LabeledGraph;
import com.ibm.wala.util.graph.labeled.SlowSparseNumberedLabeledGraph;
import com.ibm.wala.util.intset.OrdinalSet;

public class PythonTurtleAnalysisEngine extends PythonAnalysisEngine<LabeledGraph<PythonTurtleAnalysisEngine.TurtlePath,PythonTurtleAnalysisEngine.EdgeType>> {

	public enum EdgeType { DATA, CONTROL }
	
	protected TurtleSummary turtles;

	public static JSONArray graphToJSON(LabeledGraph<TurtlePath, EdgeType> analysis) {
		int I = 0;
		Map<TurtlePath,Integer> idx = HashMapFactory.make();
		JSONArray stuff = new JSONArray();
		for(TurtlePath tp : analysis) {
			JSONObject elt = tp.toJSON();
			elt.put("edges", new JSONObject());
			stuff.put(elt);
			idx.put(tp, I++);
		}
		analysis.forEach((TurtlePath src) -> {
			analysis.getSuccLabels(src).forEachRemaining((label) -> {
				JSONArray succ = new JSONArray();
				analysis.getSuccNodes(src, label).forEachRemaining((TurtlePath dst) -> {
					succ.put(idx.get(dst));
				});
				((JSONObject)((JSONObject)stuff.get(idx.get(src))).get("edges")).put(label.toString(), succ);
			});
		});
		return stuff;
	}
	
	@Override
	protected void addBypassLogic(IClassHierarchy cha, AnalysisOptions options) {
		super.addBypassLogic(cha, options);
		addSummaryBypassLogic(options, "turtles.xml");

		turtles = new TurtleSummary(getClassHierarchy());

		turtles.analyzeWithTurtles(options);
	}

	private static String toPathElement(MemberReference ref) {
		if (ref instanceof MethodReference) {
			return ref.getDeclaringClass().getName().toString().substring(1);					
		} else {
			return ref.getName().toString();
		}
	}

	private static Object readData(Supplier<Object> getValue, Supplier<List<List<MemberReference>>> getPath) {
		Object val = getValue.get();
		if (val != null) {
			return val;
		} else {
			JSONArray arg = new JSONArray();
			getPath.get().forEach((elt) -> {
				JSONArray eltJson = new JSONArray();
				elt.forEach((name) -> {
					eltJson.put(toPathElement(name));
				});
				arg.put(eltJson);
			});
			return arg;
		}
	}
	
	public static interface TurtlePath {
		Statement statement();
		PointerKey value();
		List<MemberReference> path();
		Position position();
		List<List<MemberReference>> argumentPath(int i);
		Object argumentValue(int i);
		List<List<MemberReference>> namePath(String name);
		Object nameValue(String name);
		int arguments();
		Iterator<String> names();

		default JSONObject toJSON() {
			JSONArray path = new JSONArray();
			for(MemberReference ref : path()) {
				path.put(toPathElement(ref));
			}

			JSONObject json = new JSONObject();
			json.put("path", path);
			Position position = position();
			json.put("expr", position.toString());
			try {
				json.put("source", new SourceBuffer(position).toString());
			} catch (JSONException | IOException e) {
				assert false;
			}
			
			PointerKey v = value();
			if (v instanceof LocalPointerKey) {
				LocalPointerKey lv = (LocalPointerKey)v;
				json.put("node", ((LocalPointerKey) v).getNode().getGraphNodeId());
				json.put("vn", lv.getValueNumber());
			}

			JSONArray args = new JSONArray();
			for(int i = 0; i < arguments(); i++) {
				int ii = i;
				args.put(readData(() -> { return argumentValue(ii); }, () -> { return argumentPath(ii); }));
			}
			json.put("args", args);

			JSONObject named = new JSONObject();
			names().forEachRemaining((str) -> { named.put(str, readData(() -> { return nameValue(str); }, () -> { return namePath(str); })); });
			json.put("named",  named);
			
			return json;
		}

		default boolean hasSuffix(List<MemberReference> suffix) {
			List<MemberReference> path = path();
			if (suffix.size() > path.size()) {
				return false;
			} else {
				int d = path.size() - suffix.size();
				for(int i = suffix.size()-1; i >= 0; i--) {
					if (! (suffix.get(i).equals(path.get(i+d)))) {
						return false;
					}
				}

				return true;
			}
		}
	}

	public static boolean match(Iterator<MemberReference> path, Iterator<String> pattern) {
		if (! path.hasNext() && !pattern.hasNext()) {
			return true;
		} else if (! path.hasNext() || !pattern.hasNext()) {
			return false;
		} else {
			MemberReference pe = path.next();
			String head = toPathElement(pe);
			String pat = pattern.next();
			if ("*".equals(pat) || head.equals(pat)) {
				return match(path, pattern);
			} if ("**".equals(pat)) {
				
				Iterator2List<MemberReference> pathList = Iterator2List.toList(path);
				pathList.add(0, pe);
				
				Iterator2List<String> patternList = Iterator2List.toList(pattern);
			
				do {
					if (match(pathList.iterator(), patternList.iterator())) {
						return true;
					}
					pathList.remove(0);
				} while (pathList.size() > 0);
			}
		}
		
		return false;
	}
	
	protected static SSAInstruction caller(TurtlePath path) {
		PointerKey result = path.value();
		if (result instanceof LocalPointerKey) {
			LocalPointerKey lpk = (LocalPointerKey) result;
			return lpk.getNode().getDU().getDef(lpk.getValueNumber());
		} else {
			return null;
		}
	}

	@Override
	public LabeledGraph<TurtlePath, EdgeType> performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
		Set<TurtlePath> turtlePaths = HashSetFactory.make();
		CallGraph CG = builder.getCallGraph();
		HeapModel H = builder.getPointerAnalysis().getHeapModel();
		PointerAnalysis<InstanceKey> PA = builder.getPointerAnalysis();
		Graph<PointsToSetVariable> ptr_G = builder.getPropagationSystem().getFlowGraphIncludingImplicitConstraints();

		Map<SSAInstruction,TurtlePath> stuff = HashMapFactory.make();
		CG.forEach((cgnode) -> {
			if (cgnode.getMethod() instanceof AstMethod) {
				IR callerIR = cgnode.getIR();
				DefUse DU = cgnode.getDU();
				values: for(int vn = 1 ; vn <= callerIR.getSymbolTable().getMaxValueNumber(); vn++) {
					SSAInstruction inst = DU.getDef(vn);
					if (inst instanceof PythonInvokeInstruction /*|| inst instanceof SSAGetInstruction*/) {
						
						OrdinalSet<InstanceKey> ptrs = PA.getPointsToSet(H.getPointerKeyForLocal(cgnode, vn));
						check: {
							for(InstanceKey ptr : ptrs) {
								if (ptr.getConcreteType().getReference().equals(TurtleSummary.turtleClassRef)) {
									break check;
								}
							}
							
							continue values;
						}

						TurtlePath x = new TurtlePath() {
							private List<MemberReference> makePath(CallGraph CG, Graph<PointsToSetVariable> ptr_G, CGNode node, DefUse du, int vn) {
								SSAInstruction def = du.getDef(vn);
								if (def == null && vn <= node.getIR().getSymbolTable().getNumberOfParameters()) {
									PointerKey ak = builder.getPointerKeyForLocal(node, vn);
									PointsToSetVariable av = builder.getPropagationSystem().findOrCreatePointsToSet(ak);
									Iterator2Set<PointerKey> back = Iterator2Collection.toSet(MapIterator.map((x) -> { return x.getPointerKey(); }, ptr_G.getPredNodes(av)));
									Set<List<MemberReference>> paths = HashSetFactory.make();
									CG.getPredNodes(node).forEachRemaining((caller) -> {
										CG.getPossibleSites(caller, node).forEachRemaining((site) -> {
											for(SSAAbstractInvokeInstruction inst : caller.getIR().getCalls(site)) {
												for(int i = 0; i < inst.getNumberOfUses(); i++) {
													PointerKey b = builder.getPointerKeyForLocal(caller, inst.getUse(i));
													if (back.contains(b)) {
														paths.add(makePath(CG, ptr_G, caller, caller.getDU(), inst.getUse(i)));
													}
												}
											}
										});
									});
									if (! paths.isEmpty() ) {
										return paths.iterator().next();
									}
								} else if (def instanceof SSAAbstractInvokeInstruction) {
									if (((SSAAbstractInvokeInstruction)def).getDeclaredTarget().getName().toString().equals("import")) {
										return Collections.singletonList(((SSAAbstractInvokeInstruction)def).getDeclaredTarget());
									} else if (CG.getPossibleTargets(node, ((SSAAbstractInvokeInstruction)def).getCallSite()).toString().contains("turtle")) {
										return makePath(CG, ptr_G, node, du, ((SSAAbstractInvokeInstruction)def).getReceiver());
									}
								} else if (def instanceof SSAGetInstruction && !(def instanceof AstGlobalRead)) {
									List<MemberReference> stuff = new LinkedList<>(makePath(CG, ptr_G, node, du, ((SSAGetInstruction)def).getRef()));
									stuff.add(0, ((SSAGetInstruction)def).getDeclaredField());
									return stuff;
								} else if (def instanceof AstLexicalRead) {
									PointerKey ak = builder.getPointerKeyForLocal(node, vn);
									PointsToSetVariable av = builder.getPropagationSystem().findOrCreatePointsToSet(ak);
									Collection<LocalPointerKey> back = HashSetFactory.make();
									ptr_G.getPredNodes(av).forEachRemaining((p) -> {
										ptr_G.getPredNodes(p).forEachRemaining((pp) -> {
											if (pp.getPointerKey() instanceof LocalPointerKey) {
												LocalPointerKey lpk = (LocalPointerKey)pp.getPointerKey();
												lpk.getNode().getDU().getUses(lpk.getValueNumber()).forEachRemaining((inst) -> {
													if (inst instanceof AstLexicalWrite) {
														back.add(lpk);
													}
												});
											}
										});
									});
									if (back.size() > 0) {
										LocalPointerKey x = back.iterator().next();
									return makePath(CG, ptr_G, x.getNode(), x.getNode().getDU(), x.getValueNumber());
									}
								}

								return Collections.emptyList();
							}

							private final List<MemberReference> path = makePath(CG, ptr_G, cgnode, DU, inst.getDef());

							
							@Override
							public Iterator<String> names() {
								if (inst instanceof PythonInvokeInstruction) {
									return ((PythonInvokeInstruction)inst).getKeywords().iterator();
								} else {
									return EmptyIterator.instance();
								}
							}

							@Override
							public int arguments() {
								return inst.getNumberOfUses();
							}
							
							public Object getValue(int useVn) {
								LocalPointerKey lk = (LocalPointerKey) value();									
								DefUse du = lk.getNode().getDU();
								SymbolTable S = lk.getNode().getIR().getSymbolTable();
								if (S.isNumberConstant(useVn) || S.isStringConstant(useVn)) {
									return S.getConstantValue(useVn);
								} else {
									boolean hasAnything = false;
									JSONObject result = new JSONObject();
									for(Iterator<SSAInstruction> uses = du.getUses(useVn); uses.hasNext(); ) {
										SSAInstruction use = uses.next();
										int val, ref;
										Object field;
										if (use instanceof SSAPutInstruction) {
											val = ((SSAPutInstruction)use).getVal();
											ref = ((SSAPutInstruction)use).getRef();
											field = ((SSAPutInstruction)use).getDeclaredField().getName();
										} else if (use instanceof PythonPropertyWrite) {
											val = ((PythonPropertyWrite)use).getValue();
											ref = ((PythonPropertyWrite)use).getObjectRef();
											if (S.isConstant(((PythonPropertyWrite)use).getMemberRef())) {
												field = S.getConstantValue(((PythonPropertyWrite)use).getMemberRef());
											} else {
												continue;
											}
										} else {
											continue;
										}
										if (ref != useVn) {
											continue;
										}
										
										if (S.isConstant(val)) {
											result.put(field.toString(), S.getConstantValue(val));
											hasAnything = true;
										} else {
											if (du.getDef(val) !=  null && 
												lk.getNode().getMethod() instanceof AstMethod &&
												du.getDef(val).iIndex() >= 0) {
												Position p = ((AstMethod)lk.getNode().getMethod()).debugInfo().getInstructionPosition(du.getDef(val).iIndex());
													SourceBuffer b;
													try {
														b = new SourceBuffer(p);
														String expr = b.toString();
														Integer ival = PythonInterpreter.interpretAsInt(expr);
														if (ival != null) {
															result.put(String.valueOf(field), ival);
															hasAnything = true;
															continue;
														}
													} catch (IOException e) {
														// TODO Auto-generated catch block
														e.printStackTrace();
													}
											} 
											
											Object value = getValue(val);
											if (value != null) {
												result.put(String.valueOf(field), value);
												hasAnything = true;												}
											}	
									}
									if (hasAnything) {
										return result;
									}
								}
								return null;
							}

							@Override
							public Object argumentValue(int i) {
								if (i <= inst.getNumberOfUses()) {
									int useVn = inst.getUse(i);
									return getValue(useVn);
								} else {
									return null;
								}
							}

							@Override
							public Object nameValue(String name) {
								if (inst instanceof PythonInvokeInstruction) {
									PythonInvokeInstruction pi = (PythonInvokeInstruction) inst;
									int useVn = pi.getUse(name);
									if (useVn != -1) {
										return getValue(useVn);
									}
								}
								
								return null;	
							}

							@Override
							public List<List<MemberReference>> argumentPath(int i) {
								if (i <= inst.getNumberOfUses()) {
									int useVn = inst.getUse(i);
									return getPath(useVn);
								} else {
									return null;
								}
							}
							
							@Override
							public List<List<MemberReference>> namePath(String name) {
								if (inst instanceof PythonInvokeInstruction) {
									PythonInvokeInstruction pi = (PythonInvokeInstruction) inst;
									int useVn = pi.getUse(name);
									if (useVn != -1) {
										return getPath(useVn);
									}
								}
								
								return null;	
							}
							
							public List<List<MemberReference>> getPath(int useVn) {
								List<List<MemberReference>> result = new LinkedList<>();
								PointerKey k = value();
								if (k instanceof LocalPointerKey) {
									LocalPointerKey lk = (LocalPointerKey)k;									

									PointerKey ak = H.getPointerKeyForLocal(lk.getNode(), useVn);
									OrdinalSet<? extends InstanceKey> ptrs = builder.getPointerAnalysis().getPointsToSet(ak);
									for(InstanceKey ptr : ptrs) {
										if (ptr.getConcreteType().getReference().equals(TurtleSummary.turtleClassRef)) {
											ptr.getCreationSites(CG).forEachRemaining((site) -> {
												CG.getPredNodes(site.fst).forEachRemaining((caller) -> {
													CG.getPossibleSites(caller, site.fst).forEachRemaining((cs) -> {
														for(SSAAbstractInvokeInstruction call : caller.getIR().getCalls(cs)) {
															result.add(makePath(CG, ptr_G, caller, caller.getDU(), call.getDef()));
														}
													});
												});
											});
										}
									}
								}
								return result;
							}	

							@Override
							public PointerKey value() {
								return builder.getPointerKeyForLocal(cgnode, inst.getDef());
							}

							@Override
							public List<MemberReference> path() {
								return path;
							}

							@Override
							public Position position() {
								return ((AstMethod)callerIR.getMethod()).debugInfo().getInstructionPosition(inst.iIndex());
							}

							@Override
							public String toString() {
								StringBuffer out = new StringBuffer();
								try {
									out.append(new SourceBuffer(((AstMethod)callerIR.getMethod()).debugInfo().getInstructionPosition(inst.iIndex())));
								} catch (IOException e) {
									out.append("v").append(inst.getDef());
								}
								out.append(":");
								out.append(path());
								return out.toString();
							}

							@Override
							public Statement statement() {
								if (value() instanceof LocalPointerKey) {
									LocalPointerKey lpk = (LocalPointerKey)value();
									SSAInstruction inst = lpk.getNode().getDU().getDef(lpk.getValueNumber());
									if (inst != null) {
										return new NormalStatement(lpk.getNode(), inst.iIndex());
									}
								}
								
								return null;
							}
						};
						turtlePaths.add(x);
						stuff.put(inst, x);
					}
				}
			}
		});
		
		LabeledGraph<TurtlePath,EdgeType> G = new SlowSparseNumberedLabeledGraph<>(EdgeType.DATA);
		turtlePaths.forEach((t) -> {
			G.addNode(t);
		});
		stuff.entrySet().forEach((i) -> {
			CGNode n = ((LocalPointerKey)i.getValue().value()).getNode();
			for(int x = 0; x < i.getKey().getNumberOfUses(); x++) {
				SSAInstruction inst = i.getKey();
				PointerKey ak = H.getPointerKeyForLocal(n, inst.getUse(x));
				OrdinalSet<? extends InstanceKey> ptrs = builder.getPointerAnalysis().getPointsToSet(ak);
				for(InstanceKey ptr : ptrs) {
					if (ptr.getConcreteType().getReference().equals(TurtleSummary.turtleClassRef)) {
						ptr.getCreationSites(CG).forEachRemaining((site) -> {
							CG.getPredNodes(site.fst).forEachRemaining((caller) -> {
								CG.getPossibleSites(caller, site.fst).forEachRemaining((cs) -> {
									for(SSAAbstractInvokeInstruction call : caller.getIR().getCalls(cs)) {
										if (stuff.containsKey(call)) {
											G.addEdge(stuff.get(call), i.getValue(), EdgeType.DATA);
										}
									}
								});
							});
						});
					}
				}
			}
		});
		
		ExplodedInterproceduralCFG ipcfg = ExplodedInterproceduralCFG.make(CG);
		ipcfg.forEach((b) -> {
//			System.err.println(b);
		});

		Function<TurtlePath, BasicBlockInContext<IExplodedBasicBlock>> toBlock = (srcPath) -> {
			LocalPointerKey rk = (LocalPointerKey) srcPath.value();
			IExplodedBasicBlock rbb = ipcfg.getCFG(rk.getNode()).getBlockForInstruction(rk.getNode().getDU().getDef(rk.getValueNumber()).iIndex());
			BasicBlockInContext<IExplodedBasicBlock> src = new BasicBlockInContext<>(rk.getNode(), rbb);
			assert ipcfg.containsNode(src) : src;
			return src;
		};

		Map<BasicBlockInContext<IExplodedBasicBlock>, TurtlePath> ipcfgNodes = HashMapFactory.make();
		turtlePaths.forEach((srcPath) -> {
			ipcfgNodes.put(toBlock.apply(srcPath), srcPath);
		});
		Graph<BasicBlockInContext<IExplodedBasicBlock>> ipcfgSlice = GraphSlicer.project(ipcfg, (x) -> { return ipcfgNodes.containsKey(x); });
		
		class GraphMapper<F, T> extends AbstractGraph<F> {
			private final Graph<T> graph;
			private final Function<F, T> map;
			private final Function<T, F> reverseMap;
						
			public GraphMapper(Graph<T> graph, Function<F, T> map, Function<T, F> reverseMap) {
				this.graph = graph;
				this.map = map;
				this.reverseMap = reverseMap;
			}

			@Override
			protected NodeManager<F> getNodeManager() {
				return new NodeManager<F>() {

					@Override
					public Stream<F> stream() {
						Iterable<F> iterable = () -> iterator();
						return StreamSupport.stream(iterable.spliterator(), false);
					}

					@Override
					public Iterator<F> iterator() {
						return new MapIterator<>(graph.iterator(), reverseMap::apply);
					}

					@Override
					public int getNumberOfNodes() {
						return graph.getNumberOfNodes();
					}

					@Override
					public void addNode(F n) {
						graph.addNode(map.apply(n));
					}

					@Override
					public void removeNode(F n) throws UnsupportedOperationException {
						graph.removeNode(map.apply(n));
					}

					@Override
					public boolean containsNode(F n) {
						return graph.containsNode(map.apply(n));
					}
					
				};
			}

			@Override
			protected EdgeManager<F> getEdgeManager() {
				return new EdgeManager<F>() {

					@Override
					public Iterator<F> getPredNodes(F n) {
						return new MapIterator<>(graph.getPredNodes(map.apply(n)), reverseMap::apply);
					}

					@Override
					public int getPredNodeCount(F n) {
						return graph.getPredNodeCount(map.apply(n));
					}

					@Override
					public Iterator<F> getSuccNodes(F n) {
						return new MapIterator<>(graph.getSuccNodes(map.apply(n)), reverseMap::apply);
					}

					@Override
					public int getSuccNodeCount(F n) {
						return graph.getSuccNodeCount(map.apply(n));
					}

					@Override
					public void addEdge(F src, F dst) {
						 graph.hasEdge(map.apply(src), map.apply(dst));
					}

					@Override
					public void removeEdge(F src, F dst) throws UnsupportedOperationException {
						 graph.removeEdge(map.apply(src), map.apply(dst));
					}

					@Override
					public void removeAllIncidentEdges(F node) throws UnsupportedOperationException {
						graph.removeAllIncidentEdges(map.apply(node));
					}

					@Override
					public void removeIncomingEdges(F node) throws UnsupportedOperationException {
						graph.removeIncomingEdges(map.apply(node));
					}

					@Override
					public void removeOutgoingEdges(F node) throws UnsupportedOperationException {
						graph.removeOutgoingEdges(map.apply(node));
					}

					@Override
					public boolean hasEdge(F src, F dst) {
						return graph.hasEdge(map.apply(src), map.apply(dst));
					}
					
				};
			}
		};
		
		Graph<TurtlePath> turtleCfg = new GraphMapper<>(ipcfgSlice, toBlock, ipcfgNodes::get);

		turtleCfg.forEach((src) -> {
			turtleCfg.getSuccNodes(src).forEachRemaining((dst) -> {
				G.addEdge(src, dst, EdgeType.CONTROL);
			});
		});
		
		return G;
	}
}
