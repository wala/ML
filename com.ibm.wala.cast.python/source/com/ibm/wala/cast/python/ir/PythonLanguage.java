package com.ibm.wala.cast.python.ir;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.ibm.wala.analysis.typeInference.PrimitiveType;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.cast.python.types.PythonTypes;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.ipa.callgraph.impl.FakeRootClass;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

public class PythonLanguage implements Language {

	public static PythonLanguage Python = new PythonLanguage();
	
	private PythonLanguage() {

	}

	@Override
	public Atom getName() {
		return PythonTypes.pythonName;
	}

	@Override
	public Language getBaseLanguage() {
		return null;
	}

	@Override
	public void registerDerivedLanguage(Language l) {
		assert false;
	}

	@Override
	public Set<Language> getDerivedLanguages() {
		return Collections.emptySet();
	}

	@Override
	public TypeReference getRootType() {
		return PythonTypes.Root;
	}

	@Override
	public TypeReference getThrowableType() {
		return PythonTypes.Exception;
	}

	@Override
	public TypeReference getConstantType(Object o) {
		// TODO: totally wrong
		return PythonTypes.Root;
	}

	@Override
	public boolean isNullType(TypeReference t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isIntType(TypeReference t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isLongType(TypeReference t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isVoidType(TypeReference t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isFloatType(TypeReference t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDoubleType(TypeReference t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isStringType(TypeReference t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isMetadataType(TypeReference t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCharType(TypeReference t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isBooleanType(TypeReference t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object getMetadataToken(Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypeReference[] getArrayInterfaces() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypeName lookupPrimitiveType(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SSAInstructionFactory instructionFactory() {
		return new JavaSourceLoaderImpl.InstructionFactory() {
		      @Override
		        public AstGlobalRead GlobalRead(int iindex, int lhs, FieldReference global) {
		          return new AstGlobalRead(iindex, lhs, global);
		        }

		        @Override
		        public AstGlobalWrite GlobalWrite(int iindex, FieldReference global, int rhs) {
		          return new AstGlobalWrite(iindex, global, rhs);
		        }
		};
	}

	@Override
	public Collection<TypeReference> inferInvokeExceptions(MethodReference target, IClassHierarchy cha)
			throws InvalidClassFileException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypeReference getStringType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TypeReference getPointerType(TypeReference pointee) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrimitiveType getPrimitive(TypeReference reference) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean methodsHaveDeclaredParameterTypes() {
		return false;
	}

	@Override
	public AbstractRootMethod getFakeRootMethod(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache) {
		return new FakeRootMethod(new FakeRootClass(PythonTypes.pythonLoader, cha), options, cache);
	}

}
