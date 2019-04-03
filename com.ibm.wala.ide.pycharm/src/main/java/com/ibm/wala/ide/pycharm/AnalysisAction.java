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
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.MarkupContent;
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
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.awt.RelativePoint;

/**
 * WALA Analysis Action
 */
class AnalysisAction extends AnAction {

	private WALAClient wala;
	
	private boolean firstTime = true;
	
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
    						int line = StringUtil.countNewLines(text.subSequence(0, offset)); // bad: copies text
    						int column = offset - StringUtil.lastIndexOf(text, '\n', 0, offset); 
							DocumentURLModule scriptModule = new DocumentURLModule(editor.getDocument());

    						if (firstTime) {
    							firstTime = false;
    							wala.wala.addSource("python", scriptModule);
    							wala.wala.analyze("python");
    						}
    						
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
     						java.lang.String m = "";
     						Either<List<Either<java.lang.String, MarkedString>>, MarkupContent> contents = t.getContents();
     						if (contents.isLeft()) {
     							for(Either<java.lang.String, MarkedString> hd : contents.getLeft()) {
     								m += (hd.isLeft()? hd.getLeft(): hd.getRight()) + "\n";
     							}
     						} else {
     							m = contents.getRight().getKind() + ": " + contents.getRight().getValue();
     						}
     						
    						JBPopupFactory.getInstance()
    		                .createHtmlTextBalloonBuilder(m, MessageType.INFO, null)
    		                .setFadeoutTime(7500)
    		                .createBalloon()
    		                .show(new RelativePoint(editor.getContentComponent(), editor.logicalPositionToXY(editor.getCaretModel().getLogicalPosition())),Balloon.Position.above);

    					} catch (IOException | java.lang.IllegalArgumentException | InterruptedException | java.util.concurrent.ExecutionException e) {
    						editor.getDocument().insertString(0, e.toString());
    					}
    				}
    			});
    		}
    	}, "analyze "  + FileDocumentManager.getInstance().getFile(editor.getDocument()).getName(), null);
    }
}
