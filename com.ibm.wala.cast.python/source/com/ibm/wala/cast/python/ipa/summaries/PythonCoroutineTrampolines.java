package com.ibm.wala.cast.python.ipa.summaries;

import com.ibm.wala.cast.loader.DynamicCallSiteReference;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;

public class PythonCoroutineTrampolines extends PythonTrampolines {
  public PythonCoroutineTrampolines(MethodTargetSelector base) {
    super(base);
  }

  PythonSummarizedFunction makeTrampoline(CGNode caller, CallSiteReference site, IClass receiver,
		MethodReference method) {
	int instIdx = 0;
	
	int i = 0;
	MethodReference synth =
	    MethodReference.findOrCreate(
	        method.getDeclaringClass(),
	        new Selector(
	            Atom.findOrCreateUnicodeAtom("$coroutine$" + receiver.getName()),
	            method.getSelector().getDescriptor()));

	SSAAbstractInvokeInstruction inst = caller.getIR().getCalls(site)[0];
	PythonSummary x = new PythonSummary(synth, inst.getNumberOfUses());

	int v = inst.getNumberOfUses()+1;
	int[] args = new int[inst.getNumberOfUses() ];
	for(i = 1; i <= inst.getNumberOfUses(); i++) {
		if (i == 1) {
			x.addStatement(PythonLanguage.Python.instructionFactory().CheckCastInstruction(instIdx++, v, i, receiver.getReference(), true));
			args[i-1] = v++;
		} else {
			args[i-1] = i;
		}
	}

	int r = i+1;
	CallSiteReference ss = new DynamicCallSiteReference(PythonTypes.CodeBody, i);
	x.addStatement(new PythonInvokeInstruction(instIdx++, r, v++, ss, args, new Pair[0]));

	x.addConstant(v, new ConstantValue("__async_content__")) ;
	x.addStatement(PythonLanguage.Python.instructionFactory().PropertyWrite(instIdx++, 1, v++, r));

	x.addStatement(PythonLanguage.Python.instructionFactory().ReturnInstruction(instIdx++, 1, false));
	
	PythonSummarizedFunction code = new PythonSummarizedFunction(synth, x, receiver);
	return code;
  }
  
	protected TypeReference specializedType() {
		return PythonTypes.AsyncCodeBody;
	}

}
