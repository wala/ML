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

import java.util.Collections;
import java.util.Map;

import com.ibm.wala.cast.python.ipa.summaries.PythonInstanceMethodTrampoline;
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
import com.ibm.wala.types.TypeReference;
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
		if (cha.isSubclassOf(receiver, cha.lookupClass(PythonTypes.object)) && receiver instanceof PythonClass) {
			if (!ctors.containsKey(receiver)) {
				TypeReference ctorRef = TypeReference.findOrCreate(receiver.getClassLoader().getReference(), receiver.getName() + "/__init__");
				IClass ctorCls = cha.lookupClass(ctorRef);
				IMethod init = ctorCls==null? null: ctorCls.getMethod(AstMethodReference.fnSelector);
				int params = init==null? 1: init.getNumberOfParameters();
				int v = params+2;
				int pc = 0;
				int inst = v++;
				MethodReference ref = MethodReference.findOrCreate(receiver.getReference(), site.getDeclaredTarget().getSelector());
				PythonSummary ctor = new PythonSummary(ref, params);
				SSAInstructionFactory insts = PythonLanguage.Python.instructionFactory();
				ctor.addStatement(insts.NewInstruction(pc, inst, NewSiteReference.make(pc, PythonTypes.object)));
				pc++;
				
				PythonClass x = (PythonClass)receiver;
				for(TypeReference r : x.getInnerReferences()) {
					int orig_t = v++;
					String typeName = r.getName().toString();
					typeName = typeName.substring(typeName.lastIndexOf('/')+1);
					FieldReference inner = FieldReference.findOrCreate(PythonTypes.Root, Atom.findOrCreateUnicodeAtom(typeName), PythonTypes.Root);
					
					ctor.addStatement(insts.GetInstruction(pc, orig_t, 1, inner));
					pc++;
					
					ctor.addStatement(insts.PutInstruction(pc, inst, orig_t, inner));
					pc++;
				}
				
				for(MethodReference r : x.getMethodReferences()) {
					int f = v++;
					ctor.addStatement(insts.NewInstruction(pc, f, NewSiteReference.make(pc, PythonInstanceMethodTrampoline.findOrCreate(r.getDeclaringClass(), receiver.getClassHierarchy()))));
					pc++;
					
					ctor.addStatement(insts.PutInstruction(pc, f, inst, FieldReference.findOrCreate(PythonTypes.Root, Atom.findOrCreateUnicodeAtom("$self"), PythonTypes.Root)));
					pc++;
					
					int orig_f = v++;
					ctor.addStatement(insts.GetInstruction(pc, orig_f, 1, FieldReference.findOrCreate(PythonTypes.Root, r.getName(), PythonTypes.Root)));
					pc++;

					ctor.addStatement(insts.PutInstruction(pc, f, orig_f, FieldReference.findOrCreate(PythonTypes.Root, Atom.findOrCreateUnicodeAtom("$function"), PythonTypes.Root)));
					pc++;

					ctor.addStatement(insts.PutInstruction(pc, inst, f, FieldReference.findOrCreate(PythonTypes.Root, r.getName(), PythonTypes.Root)));
					pc++;
				}
				
				ctor.addStatement(insts.ReturnInstruction(pc++, inst, false));
			
				ctor.setValueNames(Collections.singletonMap(1, Atom.findOrCreateUnicodeAtom("self")));
				
				ctors.put(receiver, new PythonSummarizedFunction(ref, ctor, receiver));
			}
			
			return ctors.get(receiver);
		}
		}
		return base.getCalleeTarget(caller, site, receiver);
	}

}
