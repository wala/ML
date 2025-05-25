package com.ibm.wala.cast.python.jep;

import java.util.Map;

import jep.Interpreter;
import jep.JepException;
import jep.SharedInterpreter;
import jep.python.PyObject;

public class Util {

	//
	// the JEP api provides a JNI wrapper for calling Python from Java.
	//
	// JEP is not thread safe, and using @ThreadLocal is a way to
	// ensure that any one JEP interpreter is only ever accessed from
	// a single thread
	//
	public static final ThreadLocal<Interpreter> interps = new ThreadLocal<>() {
		@Override
		protected Interpreter initialValue() {
			Interpreter interp = new SharedInterpreter();
		    interp.exec("import ast");
		    interp.exec("import ast2json");
		    interp.exec("has = hasattr");
		    interp.exec("get_value = lambda x: eval(compile(ast.Expression(x), '', 'eval'))");
		    return interp;
		} 
	};

	public static String typeName(PyObject obj) {
		Interpreter interp = interps.get();
	
		interp.set("obj", obj);
		interp.exec("objname = type(obj).__name__");
		
		return (String) interp.getValue("objname");
		
	}

	public static String moduleName(PyObject obj) {
		Interpreter interp = interps.get();
	
		interp.set("obj", obj);
		interp.exec("modname = type(obj).__module__");
		
		return (String) interp.getValue("modname");
		
	}

	public static PyObject runit(String expr) {
		Interpreter interp = interps.get();
		
		interp.exec("result = " + expr);
		
		return (PyObject) interp.getValue("result");
		
		
	}

	/**
	 * parse Python code into the standard CPython AST
	 * 
	 * @param code source code to be parsed into an AST
	 * @return AST as a @PyObject
	 */
	public static PyObject getAST(String code) {
		Interpreter interp = interps.get();
	
		interp.set("code", code);
		interp.exec("theast = ast.parse(code)");
	
		return (PyObject) interp.getValue("theast");
	}

	/**
	 * turn an AST into a JSON representation
	 * 
	 * @param ast a Python AST as a @PyObject
	 * @return JSON form of the AST as tree of @Map objects
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, ?> getJSON(PyObject ast) {
		Interpreter interp = interps.get();
	
		interp.set("theast", ast);
		interp.exec("thejson = ast2json.ast2json(theast)");
	
		return (Map<String, ?>) interp.getValue("thejson");
	
	}
	
	public static PyObject fixForCompilation(PyObject ast) {
		Interpreter interp = interps.get();
		
		try {
			return (PyObject) interp.invoke("ast.fix_missing_locations", ast);
		} catch (JepException e) {
			return null;
		}
	}
	
	public static boolean has(PyObject o, String property) {
		Interpreter interp = interps.get();

		return (Boolean) interp.invoke("has", o, property);
	}
	
	public static PyObject compile(PyObject o) {
		Interpreter interp = interps.get();
		PyObject exprNode = (PyObject) interp.invoke("ast.Expression", o);
		return (PyObject) interp.invoke("compile_ast", exprNode, "", "eval");
	}
	
	public static Object run(PyObject o) {
		Interpreter interp = interps.get();
		try {
			return (Object) interp.invoke("get_value", o);
		} catch (JepException e) {
			return null;
		}
	}
}
