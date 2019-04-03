package com.ibm.wala.cast.python.ipa.summaries;

import java.util.Map;

import com.ibm.wala.cast.loader.DynamicCallSiteReference;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.strings.Atom;

public class PythonComprehensionTrampolines implements MethodTargetSelector {
	private final MethodTargetSelector base;
	private final Map<IClass, PythonSummarizedFunction> trampolines = HashMapFactory.make();

	public PythonComprehensionTrampolines(MethodTargetSelector base) {
		this.base = base;
	}

	@Override
	public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
		MethodReference method = site.getDeclaredTarget();
		if (method.getSelector().equals(AstMethodReference.fnSelector) &&
			caller.getClassHierarchy().isSubclassOf(receiver, caller.getClassHierarchy().lookupClass(PythonTypes.comprehension)) &&
			!trampolines.values().contains(caller.getMethod())) {

			if (trampolines.containsKey(receiver)) {
				return trampolines.get(receiver);

			} else {

				MethodReference synth = 
					MethodReference.findOrCreate(
						method.getDeclaringClass(), 
						new Selector(
							Atom.findOrCreateUnicodeAtom("__" + receiver.getName()),
							method.getSelector().getDescriptor()));

				SSAAbstractInvokeInstruction inst = caller.getIR().getCalls(site)[0];
				int v = inst.getNumberOfUses() + 3;
				int[] args = new int[ inst.getNumberOfUses()-1 ];
				args[0] = 1;
				int nullVal = v++;

				PythonSummary x = new PythonSummary(synth, inst.getNumberOfUses());
				int idx = 0;

				x.addConstant(nullVal, null);

				int ofv = -1;
				for(int lst = 3; lst <= inst.getNumberOfUses(); lst++) {
					int fv = v++;
					ofv = fv;
					int lv = v++;
					x.addStatement(PythonLanguage.Python.instructionFactory().EachElementGetInstruction(idx++, fv, lst, nullVal));
					x.addStatement(PythonLanguage.Python.instructionFactory().PropertyRead(idx++, lv, lst, fv));
					args[lst-2] = lv;
				}

				int s = idx++;
				int r = v++;
				CallSiteReference ss = new DynamicCallSiteReference(PythonTypes.CodeBody, s);
				x.addStatement(new PythonInvokeInstruction(s, r, v++, ss, args, new Pair[0]));

				x.addStatement(PythonLanguage.Python.instructionFactory().PropertyWrite(idx++, 2, ofv, r));

				x.addStatement(PythonLanguage.Python.instructionFactory().ReturnInstruction(idx++, 2, false));

				PythonSummarizedFunction code = new PythonSummarizedFunction(synth, x, receiver);

				trampolines.put(receiver, code);

				return code;
			}
		}

		return base.getCalleeTarget(caller, site, receiver);
	}

}
