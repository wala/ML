package com.ibm.wala.cast.python.analysis.ap;

import java.util.List;
import java.util.Set;

import com.ibm.wala.util.collections.Pair;

public interface ICallbackAP {

	public Set<Pair<Integer, List<String>>> getCallbackParameter();
	
	public Set<Pair<Integer, List<String>>> getCalleeTaintedParameters();
	
}
