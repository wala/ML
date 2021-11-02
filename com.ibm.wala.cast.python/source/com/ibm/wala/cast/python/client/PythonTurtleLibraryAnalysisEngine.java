package com.ibm.wala.cast.python.client;

import com.ibm.wala.cast.python.ipa.summaries.TurtleSummary;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;

public class PythonTurtleLibraryAnalysisEngine extends PythonTurtleAnalysisEngine {

	@Override
	protected Iterable<Entrypoint> makeDefaultEntrypoints(IClassHierarchy cha) {
		return TurtleSummary.turtleEntryPoints(cha);
	}


}
