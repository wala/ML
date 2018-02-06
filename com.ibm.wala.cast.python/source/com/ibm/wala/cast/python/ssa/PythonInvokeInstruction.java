package com.ibm.wala.cast.python.ssa;

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;

public class PythonInvokeInstruction extends SSAAbstractInvokeInstruction {
	private final int result;
	private final int[] positionalParams;
	private final Pair<String,Integer>[] keywordParams;

	public PythonInvokeInstruction(int iindex, int result, int exception, CallSiteReference site, int[] positionalParams, Pair<String,Integer>[] keywordParams) {
		super(iindex, exception, site);
		this.positionalParams = positionalParams;
		this.keywordParams = keywordParams;
		this.result = result;
	}

	@Override
	public int getNumberOfParameters() {
		return getNumberOfUses();
	}

	public int getNumberOfPositionalParameters() {
		return positionalParams.length;
	}
	
	@Override
	public int getNumberOfUses() {
		return positionalParams.length + keywordParams.length;
	}

	public int getUse(String keyword) {
		for(int i = 0; i < keywordParams.length; i++) {
			if (keywordParams[i].fst.equals(keyword)) {
				return keywordParams[i].snd;
			}
		}
		
		assert false : "keyword " + keyword + " not found in " + this;
		return -1;
	}
	
	@Override
	public int getUse(int j) throws UnsupportedOperationException {
		if (j < positionalParams.length) {
			return positionalParams[j];
		} else {
			assert j < getNumberOfParameters();
			return keywordParams[j - positionalParams.length].snd;
		}
	}

	@Override
	public int getNumberOfReturnValues() {
		return 1;
	}

	@Override
	public int getReturnValue(int i) {
		assert i == 0;
		return result;
	}

	@Override
	public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
		int nr = defs==null || defs.length == 0? result: defs[0];
		int ne = defs==null || defs.length == 0? exception: defs[1];
		
		int[] newpos = positionalParams;
		Pair<String,Integer>[] newkey = keywordParams;
		if (uses != null && uses.length > 0) {
			int j = 0;
			newpos = new int[ positionalParams.length ];
			for(int i = 0; i < positionalParams.length; i++, j++) {
				newpos[i] = uses[j];
			}
			newkey = new Pair[ keywordParams.length ];
			for(int i = 0; i < keywordParams.length; i++, j++) {
				newkey[i] = Pair.make(keywordParams[i].fst, uses[j]);
			}
		}
		
		return new PythonInvokeInstruction(iindex, nr, ne, site, newpos, newkey);
	}

	@Override
	public void visit(IVisitor v) {
		((PythonInstructionVisitor)v).visitPythonInvoke(this);
	}

	@Override
	public int hashCode() {
		return getCallSite().hashCode() * result;
	}

	@Override
	public Collection<TypeReference> getExceptionTypes() {
		return Collections.singleton(PythonTypes.Exception);
	}

}
