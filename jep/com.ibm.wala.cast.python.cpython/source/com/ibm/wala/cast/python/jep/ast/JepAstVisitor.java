package com.ibm.wala.cast.python.jep.ast;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import com.ibm.wala.cast.python.jep.Util;
import com.ibm.wala.cast.python.jep.ast.CPythonAstToCAstTranslator.WalkContext;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.AbstractSourcePosition;

import jep.JepException;
import jep.python.PyObject;

public interface JepAstVisitor<R, S> {
	URL url();
	
	default boolean checkPosition(PyObject o) {
		try {
			o.getAttr("lineno");
			return true;
		} catch (JepException e) {
			return false;
		}
	}

	default Position pos(PyObject o) {
		if (checkPosition(o)) {
			return new AbstractSourcePosition() {

				@Override
				public int getFirstLine() {
					try {
						return ((Number) o.getAttr("lineno")).intValue();
					} catch (JepException e) {
						return -1;
					}
				}

				@Override
				public int getLastLine() {
					try {
						return ((Number) o.getAttr("end_lineno")).intValue();
					} catch (JepException e) {
						return -1;
					}

				}

				@Override
				public int getFirstCol() {
					try {
						return ((Number) o.getAttr("col_offset")).intValue();
					} catch (JepException e) {
						return -1;
					}
				}

				@Override
				public int getLastCol() {
					try {
						return ((Number) o.getAttr("end_col_offset")).intValue();
					} catch (JepException e) {
						return -1;
					}

				}

				@Override
				public int getFirstOffset() {
					return -1;
				}

				@Override
				public int getLastOffset() {
					return -1;
				}

				@Override
				public URL getURL() {
					return url();
				}

				@Override
				public Reader getReader() throws IOException {
					return new InputStreamReader(url().openConnection().getInputStream());
				}
			};
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	default R visit(PyObject o, S context) {
		try {
			assert "ast".equals(Util.moduleName(o)) : Util.moduleName(o);
			Method vm = this.getClass().getMethod("visit" + Util.typeName(o), PyObject.class, WalkContext.class);
			return (R) vm.invoke(this, o, context);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | InvocationTargetException e) {
			assert false : e;
			return null;
		}
	}
	
	default R visitModule(PyObject o, S context) {
		return null;
	}
	
}
