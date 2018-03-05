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
package com.ibm.wala.cast.python.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.cast.python.types.TensorType.Dimension;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;

public class TensorType implements Iterable<Dimension<?>> {

	enum DimensionType { Constant, Symbolic, Compound };
	
	abstract static class Dimension<T> {
		private final T v;

		protected Dimension(T v) {
			this.v = v;
		}

		abstract DimensionType type();
		
		abstract int symbolicDims();
		
		abstract int concreteSize();
		
		T value() {
			return v;
		}

		@Override
		public String toString() {
			return "D:" + type() + "," + value();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((v == null) ? 0 : v.hashCode());
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
			Dimension<?> other = (Dimension<?>) obj;
			if (v == null) {
				if (other.v != null)
					return false;
			} else if (!v.equals(other.v))
				return false;
			return true;
		}
	}
	
	static class SymbolicDim extends Dimension<String> {
		SymbolicDim(String name) {
			super(name);
		}
		
		@Override
		DimensionType type() {
			return DimensionType.Symbolic;
		}

		@Override
		int concreteSize() {
			return 1;
		}

		@Override
		int symbolicDims() {
			return 1;
		}
	}

	static class NumericDim extends Dimension<Integer> {
		NumericDim(Integer v) {
			super(v);
		}
		
		@Override
		DimensionType type() {
			return DimensionType.Constant;
		}

		@Override
		int concreteSize() {
			return value();
		}

		@Override
		int symbolicDims() {
			return 0;
		}
	}

	static class CompoundDim extends Dimension<List<Dimension<?>>> {
		CompoundDim(List<Dimension<?>> v) {
			super(v);
		}
		
		@Override
		DimensionType type() {
			return DimensionType.Compound;
		}

		@Override
		int concreteSize() {
			int size = 1;
			for(Dimension<?> x : value()) {
				size *= x.concreteSize();
			}
			return size;
		}

		@Override
		int symbolicDims() {
			int size = 0;
			for(Dimension<?> x : value()) {
				size += x.symbolicDims();
			}
			return size;
		}
	}

	private final String cellType;
	private final List<Dimension<?>> dims;
	
	public TensorType(String cellType, List<Dimension<?>> dims) {
		this.cellType = cellType;
		this.dims = dims;
	}
	
	@Override
	public String toString() {
		return "{" + dims.toString() + " of " + cellType + "}";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cellType == null) ? 0 : cellType.hashCode());
		result = prime * result + ((dims == null) ? 0 : dims.hashCode());
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
		TensorType other = (TensorType) obj;
		if (cellType == null) {
			if (other.cellType != null)
				return false;
		} else if (!cellType.equals(other.cellType))
			return false;
		if (dims == null) {
			if (other.dims != null)
				return false;
		} else if (!dims.equals(other.dims))
			return false;
		return true;
	}

	public static TensorType mnistInput() {
		Dimension<String> batch = new SymbolicDim("n");
		Dimension<Integer> x = new NumericDim(28);
		Dimension<Integer> y = new NumericDim(28);
		Dimension<List<Dimension<?>>> vec = new CompoundDim(Arrays.asList(x, y));
		return new TensorType("pixel", Arrays.asList(batch, vec));
	}
	
	public static TensorType reshapeArg(CGNode node, int literalVn) {
		ArrayList<Dimension<?>> r = new ArrayList<>();
		DefUse du = node.getDU();
		SymbolTable S = node.getIR().getSymbolTable();
		for(Iterator<SSAInstruction> uses = du.getUses(literalVn); uses.hasNext(); ) {
			SSAInstruction use = uses.next();
			if (use instanceof SSAPutInstruction && ((SSAPutInstruction)use).getRef() == literalVn) {
				int v = ((Number) S.getConstantValue(((SSAPutInstruction)use).getVal())).intValue();
//				int dim = Integer.valueOf(((SSAPutInstruction)use).getDeclaredField().getName().toString());
				r.add(v >=0? new NumericDim((Integer)v): new SymbolicDim("?")); 
			}
		}
		return new TensorType("pixel", r);
	}

	@Override
	public Iterator<Dimension<?>> iterator() {
		return dims.iterator();
	}
	
	public int symbolicDims() {
		int sz = 0;
		for(Dimension<?> d : this) {
			sz += d.symbolicDims();
		}
		return sz;
	}
	
	public int concreteSize() {
		int sz = 1;
		for(Dimension<?> d : this) {
			sz *= d.concreteSize();
		}
		return sz;
	}

}
