package com.ibm.wala.ide.pycharm;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import org.python.antlr.BaseParser;
import org.python.antlr.runtime.ANTLRReaderStream;
import org.python.antlr.runtime.CharStream;
import org.python.core.PyObject;

import com.ibm.wala.cast.python.parser.PythonParser;
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
	protected PyObject parse() throws IOException {
		CharStream file = new ANTLRReaderStream(getReader());
		BaseParser parser = new BaseParser(file, scriptName(), null);
		return parser.parseModule();
	}

	@Override
	protected java.lang.String scriptName() {
		return FileDocumentManager.getInstance().getFile(script).getName();

	}
}