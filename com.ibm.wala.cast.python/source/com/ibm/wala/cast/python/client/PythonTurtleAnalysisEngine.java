package com.ibm.wala.cast.python.client;

import com.ibm.wala.cast.python.ipa.summaries.TurtleSummary;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.util.CancelException;

public class PythonTurtleAnalysisEngine extends PythonAnalysisEngine<Void> {

	@Override
	protected void addBypassLogic(AnalysisOptions options) {
		super.addBypassLogic(options);
		addSummaryBypassLogic(options, "numpy_turtle.xml");
		
		new TurtleSummary(getClassHierarchy()).analyzeTurtles(options);
	}

	@Override
	public Void performAnalysis(PropagationCallGraphBuilder builder) throws CancelException {
		return null;
	}

}
