package com.ibm.wala.cast.python.analysis.ap;

import java.util.Set;

public class GlobalMethodAP extends GlobalVarAP implements IMethodAP {

	private final Set<Integer> interestingParameters;
	
	public GlobalMethodAP(String varName, Set<Integer> interestingParameters) {
		super(varName);
		this.interestingParameters = interestingParameters;
	}

	@Override
	public Set<Integer> getInterestingParameters() {
		return interestingParameters;
	}

}
