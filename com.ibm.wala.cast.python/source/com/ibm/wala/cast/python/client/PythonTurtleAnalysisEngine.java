package com.ibm.wala.cast.python.client;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.python.ipa.summaries.TurtleSummary;
import com.ibm.wala.cast.util.SourceBuffer;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.util.CancelException;

public class PythonTurtleAnalysisEngine extends PythonAnalysisEngine<Void> {

	private TurtleSummary turtles;
	
	@Override
	protected void addBypassLogic(AnalysisOptions options) {
		super.addBypassLogic(options);
		addSummaryBypassLogic(options, "numpy_turtle.xml");
		
		turtles = new TurtleSummary(getClassHierarchy());
		
		turtles.analyzeTurtles(options);
	}

	private List<MemberReference> path(CallGraph CG, CGNode node, DefUse du, int vn) {
		SSAInstruction def = du.getDef(vn);
		if (def instanceof SSAAbstractInvokeInstruction) {
			if (((SSAAbstractInvokeInstruction)def).getDeclaredTarget().getName().toString().equals("import")) {
				return Collections.singletonList(((SSAAbstractInvokeInstruction)def).getDeclaredTarget());
			} else if (CG.getPossibleTargets(node, ((SSAAbstractInvokeInstruction)def).getCallSite()).toString().contains("turtle")) {
				return path(CG, node, du, ((SSAAbstractInvokeInstruction)def).getReceiver());
			}
		} else if (def instanceof SSAGetInstruction) {
			List<MemberReference> stuff = new LinkedList<>(path(CG, node, du, ((SSAGetInstruction)def).getRef()));
			stuff.add(0, ((SSAGetInstruction)def).getDeclaredField());
			return stuff;
		} 
		
		return Collections.emptyList();
	}
	
	@Override
	public Void performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
		CallGraph CG = builder.getCallGraph();
		CG.getNodes(turtles.getCode().getReference()).forEach((CGNode turtle) -> {
			CG.getPredNodes(turtle).forEachRemaining((CGNode caller) -> {
				IR callerIR = caller.getIR();
				DefUse DU = caller.getDU();
				CG.getPossibleSites(caller, turtle).forEachRemaining((CallSiteReference site) -> {
					 for(SSAAbstractInvokeInstruction inst : callerIR.getCalls(site)) {
						String expr;
						try {
							expr = new SourceBuffer(((AstMethod)callerIR.getMethod()).debugInfo().getInstructionPosition(inst.iindex)).toString();
						} catch (IOException e1) {
							expr = "v" + inst.getDef();;
						}
						 System.err.println(expr + " : " + path(CG, caller, DU, inst.getReceiver()));
						 for(int i = 1; i < inst.getNumberOfUses(); i++) {
							 List<MemberReference> path = path(CG, caller, DU, inst.getUse(i));
							 if (! path.isEmpty()) {
								String name;
								try {
									name = new SourceBuffer(((AstMethod)callerIR.getMethod()).debugInfo().getOperandPosition(inst.iindex, i)).toString();
								} catch (IOException e) {
									name = "arg " + i;
								}
								 System.err.println("  " + name + ": " + path + " " + name);
							 }
						 }
					 }
				});
			});
		});
		return null;
	}

}
