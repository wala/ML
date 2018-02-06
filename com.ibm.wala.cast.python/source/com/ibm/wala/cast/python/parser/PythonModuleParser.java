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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.python.core.PyObject;

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.impl.CAstTypeDictionaryImpl;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.util.io.TemporaryFile;

public class PythonModuleParser extends PythonParser<ModuleEntry> {

	private final SourceURLModule fileName;
	
	protected URL getParsedURL() throws IOException {
		return fileName.getURL();
	}

	@Override
	protected PyObject parse() throws IOException {
		File f = File.createTempFile("wala", "py");
		f.deleteOnExit();
		TemporaryFile.streamToFile(f, fileName.getInputStream());
		return interpreter.eval("ast.parse(''.join(open('" + f.getAbsolutePath() + "')))");
	}

	public PythonModuleParser(SourceURLModule fileName, CAstTypeDictionaryImpl<PyObject> types) {
		super(types);
		this.fileName = fileName;
	}

	@Override
	protected String scriptName() {
		return fileName.getName();
	}

	public static void main(String[] args) throws Exception {
		URL url = new URL(args[0]);
		PythonParser<ModuleEntry> p = new PythonModuleParser(new SourceURLModule(url), new CAstTypeDictionaryImpl<PyObject>());
		CAstEntity script = p.translateToCAst();
		System.err.println(script);
		System.err.println(CAstPrinter.print(script));
	}

	@Override
	protected Reader getReader() throws IOException {
		return new InputStreamReader(fileName.getInputStream());
	}

}
