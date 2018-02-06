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
package com.ibm.wala.cast.python.parser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import org.python.core.PyObject;

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.impl.CAstTypeDictionaryImpl;
import com.ibm.wala.cast.util.CAstPrinter;

public class PythonFileParser extends PythonParser<File> {

	private final File fileName;
	
	public PythonFileParser(File fileName, CAstTypeDictionaryImpl<PyObject> types) {
		super(types);
		this.fileName  = fileName;
		interpreter.exec("import ast");
	}

	protected String scriptName() {
		return fileName.getName();
	}
	
	protected URL getParsedURL() throws IOException {
		return fileName.toURI().toURL();
	}
	
	protected PyObject parse() {
		return interpreter.eval("ast.parse(''.join(open('" + fileName.getAbsolutePath() + "')))");
	}

	public static void main(String[] args) throws Exception {
		PythonParser<File> p = new PythonFileParser(new File(args[0]), new CAstTypeDictionaryImpl<PyObject>());
		CAstEntity script = p.translateToCAst();
		System.err.println(script);
		System.err.println(CAstPrinter.print(script));
	}

	@Override
	protected Reader getReader() throws IOException {
		return new FileReader(fileName);
	}

}
