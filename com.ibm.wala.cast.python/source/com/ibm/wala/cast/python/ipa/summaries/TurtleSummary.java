package com.ibm.wala.cast.python.ipa.summaries;

import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;

import com.ibm.wala.cast.python.ir.PythonLanguage;
import com.ibm.wala.cast.python.ssa.PythonInvokeInstruction;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ClassTargetSelector;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.BypassSyntheticClassLoader;
import com.ibm.wala.shrikeBT.IInvokeInstruction.Dispatch;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.strings.Atom;

public class TurtleSummary {
	public static final TypeReference turtleClassRef = TypeReference.findOrCreate(PythonTypes.pythonLoader, "Lturtle");
	public static final MethodReference turtleMethodRef = MethodReference.findOrCreate(turtleClassRef, Atom.findOrCreateUnicodeAtom("turtle"), AstMethodReference.fnDesc);
	public static final FieldReference turtleFieldRef = FieldReference.findOrCreate(turtleClassRef, Atom.findOrCreateUnicodeAtom("turtle"), PythonTypes.Root);

	public static final MethodReference turtleCallbackMethodRef = MethodReference.findOrCreate(turtleClassRef, Atom.findOrCreateUnicodeAtom("callback"), AstMethodReference.fnDesc);

	private final IClassHierarchy cha;
	
	private IMethod turtleMethod = new IMethod() {

		@Override
		public IClass getDeclaringClass() {
			return turtleClass;
		}

		@Override
		public Atom getName() {
			return getReference().getName();
		}

		@Override
		public boolean isStatic() {
			return false;
		}

		@Override
		public Collection<Annotation> getAnnotations() {
			return Collections.emptySet();
		}

		@Override
		public IClassHierarchy getClassHierarchy() {
			return cha;
		}

		@Override
		public boolean isSynchronized() {
			return false;
		}

		@Override
		public boolean isClinit() {
			return false;
		}

		@Override
		public boolean isInit() {
			return false;
		}

		@Override
		public boolean isNative() {
			return false;
		}

		@Override
		public boolean isWalaSynthetic() {
			return true;
		}

		@Override
		public boolean isSynthetic() {
			return true;
		}

		@Override
		public boolean isAbstract() {
			return false;
		}

		@Override
		public boolean isPrivate() {
			return false;
		}

		@Override
		public boolean isProtected() {
			return false;
		}

		@Override
		public boolean isPublic() {
			return true;
		}

		@Override
		public boolean isFinal() {
			return false;
		}

		@Override
		public boolean isBridge() {
			return false;
		}

		@Override
		public MethodReference getReference() {
			return turtleMethodRef;
		}

		@Override
		public boolean hasExceptionHandler() {
			return false;
		}

		@Override
		public TypeReference getParameterType(int i) {
			return PythonTypes.Root;
		}

		@Override
		public TypeReference getReturnType() {
			return PythonTypes.Root;
		}

		@Override
		public int getNumberOfParameters() {
			return 0;
		}

		@Override
		public TypeReference[] getDeclaredExceptions() throws InvalidClassFileException, UnsupportedOperationException {
			return new TypeReference[0];
		}

		@Override
		public int getLineNumber(int bcIndex) {
			return -1;
		}

		@Override
		public SourcePosition getSourcePosition(int instructionIndex) throws InvalidClassFileException {
			return null;
		}

		@Override
		public SourcePosition getParameterSourcePosition(int paramNum) throws InvalidClassFileException {
			return null;
		}

		@Override
		public String getLocalVariableName(int bcIndex, int localNumber) {
			return null;
		}

		@Override
		public String getSignature() {
			return getReference().getSignature();
		}

		@Override
		public Selector getSelector() {
			return getReference().getSelector();
		}

		@Override
		public Descriptor getDescriptor() {
			return getReference().getDescriptor();
		}

		@Override
		public boolean hasLocalVariableTable() {
			return false;
		}		
	};
	
	private IField turtleField = new IField() {

		@Override
		public IClass getDeclaringClass() {
			return turtleClass;
		}

		@Override
		public Atom getName() {
			return getReference().getName();
		}

		@Override
		public Collection<Annotation> getAnnotations() {
			return Collections.emptySet();
		}

		@Override
		public IClassHierarchy getClassHierarchy() {
			return cha;
		}

		@Override
		public TypeReference getFieldTypeReference() {
			return getReference().getFieldType();
		}

		@Override
		public FieldReference getReference() {
			return turtleFieldRef;
		}

		@Override
		public boolean isFinal() {
			return false;
		}

		@Override
		public boolean isPrivate() {
			return false;
		}

		@Override
		public boolean isProtected() {
			return false;
		}

		@Override
		public boolean isPublic() {
			return true;
		}

		@Override
		public boolean isStatic() {
			return false;
		}

		@Override
		public boolean isVolatile() {
			return false;
		}
	};
	
	private IClass turtleClass = new IClass() {

		@Override
		public IClassHierarchy getClassHierarchy() {
			return cha;
		}

		@Override
		public IClassLoader getClassLoader() {
			return cha.getLoader(PythonTypes.pythonLoader);
		}

		@Override
		public boolean isInterface() {
			return false;
		}

		@Override
		public boolean isAbstract() {
			return false;
		}

		@Override
		public boolean isPublic() {
			return true;
		}

		@Override
		public boolean isPrivate() {
			return false;
		}

		@Override
		public boolean isSynthetic() {
			return true;
		}

		@Override
		public int getModifiers() throws UnsupportedOperationException {
			return 0;
		}

		@Override
		public IClass getSuperclass() {
			return cha.lookupClass(PythonTypes.Root);
		}

		@Override
		public Collection<? extends IClass> getDirectInterfaces() {
			return Collections.emptySet();
		}

		@Override
		public Collection<IClass> getAllImplementedInterfaces() {
			return Collections.emptySet();
		}

		@Override
		public IMethod getMethod(Selector selector) {
			if (selector.equals(MethodReference.clinitSelector) || 
				selector.equals(MethodReference.initSelector) || 
				selector.equals(MethodReference.finalizeSelector)) {
				return null;
			} else {
				return turtleMethod;
			}
		}

		@Override
		public IField getField(Atom name) {
			return turtleField;
		}

		@Override
		public IField getField(Atom name, TypeName type) {
			return getField(name);
		}

		@Override
		public TypeReference getReference() {
			return turtleClassRef;
		}

		@Override
		public String getSourceFileName() throws NoSuchElementException {
			throw new NoSuchElementException();
		}

		@Override
		public Reader getSource() throws NoSuchElementException {
			throw new NoSuchElementException();
		}

		@Override
		public IMethod getClassInitializer() {
			return null;
		}

		@Override
		public boolean isArrayClass() {
			return false;
		}

		@Override
		public Collection<? extends IMethod> getDeclaredMethods() {
			return Collections.emptySet();
		}

		@Override
		public Collection<IField> getAllInstanceFields() {
			return Collections.emptySet();
		}

		@Override
		public Collection<IField> getAllStaticFields() {
			return Collections.emptySet();
		}

		@Override
		public Collection<IField> getAllFields() {
			return Collections.emptySet();
		}

		@Override
		public Collection<? extends IMethod> getAllMethods() {
			return Collections.emptySet();
		}

		@Override
		public Collection<IField> getDeclaredInstanceFields() {
			return Collections.emptySet();
		}

		@Override
		public Collection<IField> getDeclaredStaticFields() {
			return Collections.emptySet();
		}

		@Override
		public TypeName getName() {
			return getReference().getName();
		}

		@Override
		public boolean isReferenceType() {
			return true;
		}

		@Override
		public Collection<Annotation> getAnnotations() {
			return Collections.emptySet();
		}	
	};

	private final IMethod code;

	public TurtleSummary(IClassHierarchy cha) {
		this.cha = cha;

		PythonSummary x = new PythonSummary(turtleMethodRef, 1);
		x.addStatement(PythonLanguage.Python.instructionFactory().NewInstruction(0, 10, NewSiteReference.make(0, turtleClassRef)));
		x.addStatement(PythonLanguage.Python.instructionFactory().PutInstruction(1, 10, 10, turtleFieldRef));
		x.addStatement(new PythonInvokeInstruction(2, 11, 12, CallSiteReference.make(2, turtleCallbackMethodRef, Dispatch.VIRTUAL), new int[] {2}, new Pair[0]));
		x.addStatement(PythonLanguage.Python.instructionFactory().ReturnInstruction(3, 10, false));
		code = new PythonSummarizedFunction(turtleMethodRef, x, turtleClass);
		
		BypassSyntheticClassLoader ldr = (BypassSyntheticClassLoader) cha.getLoader(cha.getScope().getSyntheticLoader());
		ldr.registerClass(turtleClassRef.getName(), turtleClass);
	}
	
	private class PythonMethodTurtleTargetSelector implements MethodTargetSelector {
		private final MethodTargetSelector base;

		public PythonMethodTurtleTargetSelector(MethodTargetSelector base) {
			this.base = base;
		}

		@Override
		public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
			if (site.getDeclaredTarget().equals(turtleCallbackMethodRef)) {
				if (caller.getClassHierarchy().isSubclassOf(receiver, caller.getClassHierarchy().lookupClass(PythonTypes.CodeBody))) {
					return receiver.getMethod(AstMethodReference.fnSelector);
				} else {
					return null;
				}
			} else if (receiver == null? site.getDeclaredTarget().getDeclaringClass().equals(turtleClassRef): receiver.equals(turtleClass)) {
				return code;
			} else {			
			    return base.getCalleeTarget(caller, site, receiver);
			}
		}
	}
	
	private class PythonClassTurtleTargetSelector implements ClassTargetSelector {
		private final ClassTargetSelector base;
		
		private PythonClassTurtleTargetSelector(ClassTargetSelector base) {
			this.base = base;
		}
		
		@Override
		public IClass getAllocatedTarget(CGNode caller, NewSiteReference site) {
			if (site.getDeclaredType().equals(turtleClassRef)) {
				return turtleClass;
			} else {
				return base.getAllocatedTarget(caller, site);
			}
		}
		
	}
	
	public IMethod getCode() {
		return code;
	}

	public void analyzeWithTurtles(AnalysisOptions options) {
		options.setSelector(new PythonMethodTurtleTargetSelector(options.getMethodTargetSelector()));
		options.setSelector(new PythonClassTurtleTargetSelector(options.getClassTargetSelector()));
	}
}
