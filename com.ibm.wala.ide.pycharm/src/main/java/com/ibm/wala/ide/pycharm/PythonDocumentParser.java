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
package com.ibm.wala.ide.pycharm;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import org.python.antlr.BaseParser;
import org.python.antlr.runtime.ANTLRReaderStream;
import org.python.antlr.runtime.CharStream;
import org.python.core.PyObject;

import com.ibm.wala.cast.python.parser.PythonParser;
import com.ibm.wala.cast.python.parser.WalaPythonParser;
import com.ibm.wala.cast.tree.impl.CAstTypeDictionaryImpl;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.util.text.CharSequenceReader;

class PythonDocumentParser extends PythonParser<Document> {
	private final Document script;
	
	protected PythonDocumentParser(Document script, CAstTypeDictionaryImpl<PyObject> types) {
		super(types);
		this.script = script;
	}

	@Override
	protected URL getParsedURL() throws IOException {
		return new URL(FileDocumentManager.getInstance().getFile(script).getUrl());
	}

	@Override
	protected Reader getReader() throws IOException {
		return new CharSequenceReader(script.getCharsSequence());
	}

	@Override
	protected java.lang.String scriptName() {
		return FileDocumentManager.getInstance().getFile(script).getName();

	}

	@Override
	protected WalaPythonParser makeParser() throws IOException {
		CharStream file = new ANTLRReaderStream(getReader());
		return new WalaPythonParser(file, scriptName(), null);
	}
}