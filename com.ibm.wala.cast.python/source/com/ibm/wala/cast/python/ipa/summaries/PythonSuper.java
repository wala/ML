package com.ibm.wala.cast.python.ipa.summaries;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.cast.ir.ssa.AstInstructionFactory;
import com.ibm.wala.cast.loader.DynamicCallSiteReference;
import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.loader.PythonLoader.PythonClass;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.DelegatingContext;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DelegatingSSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.strings.Atom;

public class PythonSuper {

	private final IClassHierarchy cha;
	
	private final IMethod superStub;
	
	public PythonSuper(IClassHierarchy cha) {
		this.cha = cha;
		this.superStub = new SyntheticMethod(AstMethodReference.fnReference(PythonTypes.superfun), cha.lookupClass(PythonTypes.superfun), false, false);
	}

	private final ContextKey superClass = new ContextKey() {
		public String toString() {
			return "Super class";
		}
	};

	private final ContextKey superSelf = new ContextKey() {
		public String toString() {
			return "Super self";
		}
	};

	private final Context implicitSuperContext = new Context() {

		@Override
		public String toString() {
			return "super call";
		}

		@Override
		public ContextItem get(ContextKey name) {
			return null;
		}
		
	};
	
	private class ExplicitSuperContext extends Pair<IClass, InstanceKey> implements Context {

		private static final long serialVersionUID = -7679970297641533615L;

		private ExplicitSuperContext(IClass fst, InstanceKey snd) {
			super(fst, snd);
		}

		@Override
		public ContextItem get(ContextKey name) {
			if (superClass.equals(name)) {
				return new ContextItem.Value<>(fst);
			} else if (superSelf.equals(name)) {
				return new ContextItem.Value<>(snd);
			} else {
				return null;
			}
		}
	}
	
	private class SuperContextInterpreter implements SSAContextInterpreter {
		private final FieldReference $class = FieldReference.findOrCreate(PythonTypes.Root, Atom.findOrCreateUnicodeAtom("$class"), PythonTypes.Root);
		private final FieldReference $self = FieldReference.findOrCreate(PythonTypes.Root, Atom.findOrCreateUnicodeAtom("$self"), PythonTypes.Root);

		@Override
		public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
			return EmptyIterator.instance();
		}

		@Override
		public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
			return Arrays.asList($class, $self).iterator();
		}

		@Override
		public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
			return EmptyIterator.instance();
		}

		@Override
		public boolean recordFactoryType(CGNode node, IClass klass) {
			return false;
		}

		@Override
		public boolean understands(CGNode node) {
			return superStub.equals(node.getMethod());
		}

		@Override
		public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
			return EmptyIterator.instance();
		}

		private final Map<IClass,PythonSummarizedFunction> ctors = HashMapFactory.make();
		private final Map<CGNode,PythonSummarizedFunction> trampolines = HashMapFactory.make();

		@Override
		public IR getIR(CGNode node) {
			int v = 4;
			int pc = 0;
			if (node.getContext().get(superClass) == null) {
				if (! trampolines.containsKey(node)) {
					PythonSummary ctor = new PythonSummary(node.getMethod().getReference(), 1);

					AstInstructionFactory insts = PythonLanguage.Python.instructionFactory();

					int cls = v++;
					ctor.addStatement(insts.GetInstruction(pc++, cls, 1, $class));

					int self = v++;
					ctor.addStatement(insts.GetInstruction(pc++, self, 1, $self));

					int result = v++;
					int except = v++;
					CallSiteReference ref = new DynamicCallSiteReference(AstMethodReference.fnReference(PythonTypes.superfun), pc++);
					ctor.addStatement(new PythonInvokeInstruction(2, result, except, ref, new int[] {1, cls, self}, new Pair[0]));

					ctor.addStatement(insts.ReturnInstruction(pc++, result, false));
					
					trampolines.put(node, new PythonSummarizedFunction(node.getMethod().getReference(), ctor, cha.lookupClass(PythonTypes.superfun)));
				}

				return trampolines.get(node).makeIR(node.getContext(), SSAOptions.defaultOptions());

			} else {
				@SuppressWarnings("unchecked")
				IClass receiver = ((ContextItem.Value<IClass>) node.getContext().get(superClass)).getValue();
				PythonClass x = (PythonClass)receiver.getSuperclass();

				if (! ctors.containsKey(x)) {
					PythonSummary ctor = new PythonSummary(node.getMethod().getReference(), 3);

					AstInstructionFactory insts = PythonLanguage.Python.instructionFactory();

					int inst = v++;
					ctor.addStatement(insts.NewInstruction(pc++, inst, NewSiteReference.make(pc, PythonTypes.object)));

					int clss = v++;
					ctor.addStatement(insts.GlobalRead(pc++, clss, FieldReference.findOrCreate(PythonTypes.Root, Atom.findOrCreateUnicodeAtom("global " + x.getName().toString().substring(1)), PythonTypes.Root)));

					for(TypeReference r : x.getInnerReferences()) {
						int orig_t = v++;
						String typeName = r.getName().toString();
						typeName = typeName.substring(typeName.lastIndexOf('/')+1);
						FieldReference inner = FieldReference.findOrCreate(PythonTypes.Root, Atom.findOrCreateUnicodeAtom(typeName), PythonTypes.Root);

						ctor.addStatement(insts.GetInstruction(pc, orig_t, clss, inner));
						pc++;

						ctor.addStatement(insts.PutInstruction(pc, inst, orig_t, inner));
						pc++;
					}

					for(MethodReference r : x.getMethodReferences()) {
						int f = v++;
						ctor.addStatement(insts.NewInstruction(pc, f, NewSiteReference.make(pc, PythonInstanceMethodTrampoline.findOrCreate(r.getDeclaringClass(), receiver.getClassHierarchy()))));
						pc++;

						ctor.addStatement(insts.PutInstruction(pc, f, 3, FieldReference.findOrCreate(PythonTypes.Root, Atom.findOrCreateUnicodeAtom("$self"), PythonTypes.Root)));
						pc++;

						int orig_f = v++;
						ctor.addStatement(insts.GetInstruction(pc, orig_f, clss, FieldReference.findOrCreate(PythonTypes.Root, r.getName(), PythonTypes.Root)));
						pc++;

						ctor.addStatement(insts.PutInstruction(pc, f, orig_f, FieldReference.findOrCreate(PythonTypes.Root, Atom.findOrCreateUnicodeAtom("$function"), PythonTypes.Root)));
						pc++;

						ctor.addStatement(insts.PutInstruction(pc, inst, f, FieldReference.findOrCreate(PythonTypes.Root, r.getName(), PythonTypes.Root)));
						pc++;
					}

					ctor.addStatement(insts.ReturnInstruction(pc++, inst, false));

					ctors.put(x, new PythonSummarizedFunction(node.getMethod().getReference(), ctor, receiver));
				}

				return ctors.get(x).makeIR(node.getContext(), SSAOptions.defaultOptions());
			}
		}

		@Override
		public IRView getIRView(CGNode node) {
			return getIR(node);
		}

		@Override
		public DefUse getDU(CGNode node) {
			return new DefUse(getIR(node));
		}

		@Override
		public int getNumberOfStatements(CGNode node) {
			return getIR(node).getInstructions().length;
		}

		@Override
		public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode n) {
			return getIR(n).getControlFlowGraph();
		}
		
	}
	
	private class SuperContextSelector implements ContextSelector {
		private final ContextSelector base;
		
		public SuperContextSelector(ContextSelector base) {
			this.base = base;
		}

		@Override
		public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee,
				InstanceKey[] actualParameters) {
			if (superStub.equals(callee)) {
				if (actualParameters.length >= 3 && actualParameters[1].getConcreteType() instanceof PythonClass) {
					return new ExplicitSuperContext(actualParameters[1].getConcreteType(), actualParameters[2]);
				} if (actualParameters.length == 1) {
					return new DelegatingContext(implicitSuperContext, base.getCalleeTarget(caller, site, callee, actualParameters));
				} else {
					return null;
				}
			} else {
				return base.getCalleeTarget(caller, site, callee, actualParameters);
			}
		}

		@Override
		public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
			SSAAbstractInvokeInstruction inst = caller.getIR().getCalls(site)[0];
			if (inst.getNumberOfUses() > 2) {
				int obj = inst.getUse(0);
				SSAInstruction x = caller.getDU().getDef(obj);
				if (x instanceof SSANewInstruction && ((SSANewInstruction)x).getConcreteType().equals(PythonTypes.superfun)) {
					return IntSetUtil.make(new int[] {0, 1, 2});
				}
			}
			
			return EmptyIntSet.instance;
		}
		
	}
	private class SuperMethodTargetSelector implements MethodTargetSelector {
		private final MethodTargetSelector base;
		
		private SuperMethodTargetSelector(MethodTargetSelector base) {
			this.base = base;
		}

		@Override
		public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
			if (receiver != null && cha.lookupClass(PythonTypes.superfun).equals(receiver)) {
				return superStub;
			} else {
				return base.getCalleeTarget(caller, site, receiver);
			}
		}
	}
	
	public void handleSuperCalls(SSAPropagationCallGraphBuilder builder, AnalysisOptions options) {
		builder.setContextInterpreter(new DelegatingSSAContextInterpreter(new SuperContextInterpreter(), builder.getCFAContextInterpreter()));;
		builder.setContextSelector(new SuperContextSelector(builder.getContextSelector()));
		options.setSelector(new SuperMethodTargetSelector(options.getMethodTargetSelector()));
	}
}
