/******************************************************************************
 * Copyright (c) 2018 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.python.ipa.callgraph;

import java.util.Map;

import com.ibm.wala.cast.loader.DynamicCallSiteReference;
import com.ibm.wala.cast.python.ipa.summaries.PythonSummarizedFunction;
import com.ibm.wala.cast.python.ipa.summaries.PythonSummary;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.strings.Atom;

public class PythonTrampolineTargetSelector implements MethodTargetSelector {
	private final MethodTargetSelector base;
	

	public PythonTrampolineTargetSelector(MethodTargetSelector base) {
		this.base = base;
	}

	private final Map<Pair<IClass,Integer>, IMethod> codeBodies = HashMapFactory.make();
	
	@Override
	public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
		System.err.println(receiver);
		if (receiver != null) {
		IClassHierarchy cha = receiver.getClassHierarchy();
		if (cha.isSubclassOf(receiver, cha.lookupClass(PythonTypes.trampoline))) {
			SSAAbstractInvokeInstruction call = caller.getIR().getCalls(site)[0];
			Pair<IClass,Integer> key = Pair.make(receiver,  call.getNumberOfParameters());
			if (!codeBodies.containsKey(key)) {
				MethodReference tr = MethodReference.findOrCreate(receiver.getReference(),
						Atom.findOrCreateUnicodeAtom("trampoline" + call.getNumberOfParameters()), 
						AstMethodReference.fnDesc);
				PythonSummary x = new PythonSummary(tr, call.getNumberOfParameters());
				int v = call.getNumberOfParameters() + 1;
				x.addStatement(PythonLanguage.Python.instructionFactory().GetInstruction(0, v, 1, FieldReference.findOrCreate(PythonTypes.Root, Atom.findOrCreateUnicodeAtom("$function"), PythonTypes.Root)));
				int v1 = v + 1;
				x.addStatement(PythonLanguage.Python.instructionFactory().GetInstruction(1, v1, 1, FieldReference.findOrCreate(PythonTypes.Root, Atom.findOrCreateUnicodeAtom("$self"), PythonTypes.Root)));
			
				int i = 0;
				int[] params = new int[ call.getNumberOfParameters()+1 ];
				params[i++] = v;
				params[i++] = v1;
				for(int j = 1; j < call.getNumberOfParameters(); j++) {
					params[i++] = j+1;
				}
				
				int result = v1 + 1;
				int except = v1 + 2;
				CallSiteReference ref = new DynamicCallSiteReference(call.getCallSite().getDeclaredTarget(), 2);
				x.addStatement(new PythonInvokeInstruction(2, result, except, ref, params, new Pair[0]));
				
				codeBodies.put(key, new PythonSummarizedFunction(tr, x, receiver));
			}
			
			return codeBodies.get(key);
		}
		}
		
		return base.getCalleeTarget(caller, site, receiver);
	}

}
