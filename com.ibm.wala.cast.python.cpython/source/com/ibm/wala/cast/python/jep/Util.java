package com.ibm.wala.cast.python.jep;

import jep.Interpreter;
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
}
