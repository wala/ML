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

import com.ibm.wala.cast.python.ipa.summaries.PythonSummarizedFunction;
import com.ibm.wala.cast.python.ipa.summaries.PythonSummary;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.loader.PythonLoader.PythonClass;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.strings.Atom;

public class PythonConstructorTargetSelector implements MethodTargetSelector {
	private final Map<IClass,IMethod> ctors = HashMapFactory.make();
	
	private final MethodTargetSelector base;
		
	public PythonConstructorTargetSelector(MethodTargetSelector base) {
		this.base = base;
	}

	@Override
	public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
		if (receiver != null) {
		IClassHierarchy cha = receiver.getClassHierarchy();
		if (cha.isSubclassOf(receiver, cha.lookupClass(PythonTypes.object))) {
			if (!ctors.containsKey(receiver)) {
				IMethod init = receiver.getMethod(new Selector(Atom.findOrCreateUnicodeAtom("__init__"), AstMethodReference.fnDesc));
				int params = init==null? 1: init.getNumberOfParameters();
				int v = params+2;
				int i = 0;
				int inst = v++;
				MethodReference ref = MethodReference.findOrCreate(receiver.getReference(), site.getDeclaredTarget().getSelector());
				PythonSummary ctor = new PythonSummary(ref, params);
				SSAInstructionFactory insts = PythonLanguage.Python.instructionFactory();
				ctor.addStatement(insts.NewInstruction(i++, inst, NewSiteReference.make(0, PythonTypes.object)));
				
				PythonClass x = (PythonClass)receiver;
				for(MethodReference r : x.getMethodReferences()) {
					int f = v++;
					int pc = i++;
					ctor.addStatement(insts.NewInstruction(pc, f, NewSiteReference.make(pc, r.getDeclaringClass())));
					ctor.addStatement(insts.PutInstruction(pc++, 1, f, FieldReference.findOrCreate(PythonTypes.Root, r.getName(), PythonTypes.Root)));
				}
				
				ctor.addStatement(insts.ReturnInstruction(i++, inst, false));
			
				ctors.put(receiver, new PythonSummarizedFunction(ref, ctor, receiver));
			}
			
			return ctors.get(receiver);
		}
		}
		return base.getCalleeTarget(caller, site, receiver);
	}

}
