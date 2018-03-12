package com.ibm.wala.ide.pycharm;

import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import com.ibm.wala.classLoader.SourceURLModule;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.util.text.CharSequenceReader;

public class DocumentURLModule extends SourceURLModule {
	private final Document script;
	
	public DocumentURLModule(Document script) throws MalformedURLException {
		super(new URL(FileDocumentManager.getInstance().getFile(script).getUrl()));
		this.script = script;
	}

	@Override
	public Reader getInputReader() {
		return new CharSequenceReader(script.getCharsSequence());
	}

	@Override
	public java.lang.String getName() {
		return FileDocumentManager.getInstance().getFile(script).getName();
	}

}
