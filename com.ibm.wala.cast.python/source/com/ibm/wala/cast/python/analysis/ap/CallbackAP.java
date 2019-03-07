package com.ibm.wala.cast.python.analysis.ap;

import java.util.List;

import com.ibm.wala.classLoader.IMethod;

public class CallbackAP implements IAPRoot {

	private final IMethod function;
	private final int parameterVn;
	private List<IPathElement> taintedPath;
	
	public List<IPathElement> getTaintedPath() {
		return taintedPath;
	}

	public CallbackAP(IMethod function, int parameterVn) {
		super();
		assert function != null;
		this.function = function;
		this.parameterVn = parameterVn;
	}

	public CallbackAP(IMethod function, Integer parameterVn, List<IPathElement> elts) {
		this(function, parameterVn);
		taintedPath = elts;
	}

	@Override
	public Kind getKind() {
		return Kind.CALLBACK;
	}

	@Override
	public int length() {
		// TODO Auto-generated method stub
		return 0;
	}

	public IMethod getFunction() {
		return function;
	}

	public int getParameterVn() {
		return parameterVn;
	}

	public String toString() {
		return "callback " + function.getDeclaringClass().getName() + " " + parameterVn;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((function == null) ? 0 : function.hashCode());
		result = prime * result + parameterVn;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CallbackAP other = (CallbackAP) obj;
		if (function == null) {
			if (other.function != null)
				return false;
		} else if (!function.equals(other.function))
			return false;
		if (parameterVn != other.parameterVn)
			return false;
		return true;
	}
	
}
