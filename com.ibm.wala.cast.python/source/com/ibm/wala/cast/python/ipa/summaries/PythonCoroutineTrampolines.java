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
	int i = 0;
	MethodReference synth =
	    MethodReference.findOrCreate(
	        method.getDeclaringClass(),
	        new Selector(
	            Atom.findOrCreateUnicodeAtom("$coroutine$" + receiver.getName()),
	            method.getSelector().getDescriptor()));

	SSAAbstractInvokeInstruction inst = caller.getIR().getCalls(site)[0];
	int v = inst.getNumberOfUses();
	int[] args = new int[inst.getNumberOfUses() ];
	for(i = 1; i <= inst.getNumberOfUses(); i++) {
		args[i-1] = i;
	}

	PythonSummary x = new PythonSummary(synth, inst.getNumberOfUses());
	int r = i+1;
	CallSiteReference ss = new DynamicCallSiteReference(PythonTypes.CodeBody, i);
	x.addStatement(new PythonInvokeInstruction(0, r, v++, ss, args, new Pair[0]));

	x.addConstant(i+2, new ConstantValue("__async_content__")) ;
	x.addStatement(PythonLanguage.Python.instructionFactory().PropertyWrite(1, 1, i+2, r));

	x.addStatement(PythonLanguage.Python.instructionFactory().ReturnInstruction(2, 1, false));
	
	PythonSummarizedFunction code = new PythonSummarizedFunction(synth, x, receiver);
	return code;
  }
  
	protected TypeReference specializedType() {
		return PythonTypes.AsyncCodeBody;
	}

}
