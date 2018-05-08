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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;

/**
 * WALA Analysis Action
 */
class AnalysisAction extends AnAction {

	private WALAClient wala;
	
	AnalysisAction() throws IOException {
		wala = new WALAClient();
	}
	
	static {
		System.setProperty("javax.xml.parsers.SAXParserFactory", "org.python.apache.xerces.jaxp.SAXParserFactoryImpl");
	}

	private Project project;
    private Editor editor;

    /**
     * Disable when no project open
     *
     * @param   event   Action system event
     */
    public void update(@NotNull AnActionEvent event) {
	this.project    = event.getData(PlatformDataKeys.PROJECT);
	this.editor     = event.getData(PlatformDataKeys.EDITOR);
	event.getPresentation().setEnabled(true);
    }

    /**
     * Perform analysis
     *
     * @param   event   Action system event
     */
    public void actionPerformed(@NotNull final AnActionEvent event) {
    	CommandProcessor.getInstance().executeCommand(project, new java.lang.Runnable() {
    		public void run() {
    			ApplicationManager.getApplication().runWriteAction(new java.lang.Runnable() {
    				public void run() {
    					try {
    						int offset = editor.logicalPositionToOffset(editor.getCaretModel().getLogicalPosition());
    						java.lang.String text = editor.getDocument().getText();
    						int line = StringUtil.countNewLines(text.subSequence(0, offset)) + 1; // bad: copies text
    						int column = offset - StringUtil.lastIndexOf(text, '\n', 0, offset); 

    						DocumentURLModule scriptModule = new DocumentURLModule(editor.getDocument());
    						wala.wala.analyze("python", scriptModule);
    						TextDocumentIdentifier id = new TextDocumentIdentifier();
    						TextDocumentPositionParams a = new TextDocumentPositionParams();
    						id.setUri(scriptModule.getURL().toString());
    						a.setTextDocument(id);
    						Position p = new Position();
    						p.setLine(line);
    						p.setCharacter(column);
    						a.setPosition(p);
    						CompletableFuture<Hover> data = wala.wala.getTextDocumentService().hover(a);
    						Hover t = data.get();
    						for(Either<java.lang.String, MarkedString> hd : t.getContents()) {
    							editor.getDocument().insertString(0, "" + hd.getLeft() + "\n");
    						}
    					} catch (IOException | java.lang.IllegalArgumentException | InterruptedException | ExecutionException e) {
    						editor.getDocument().insertString(0, e.toString());
    					}
    				}
    			});
    		}
    	}, "analyze "  + FileDocumentManager.getInstance().getFile(editor.getDocument()).getName(), null);
    }
}
