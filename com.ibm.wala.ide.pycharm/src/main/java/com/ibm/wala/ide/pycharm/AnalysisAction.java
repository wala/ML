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

import org.jetbrains.annotations.NotNull;

import com.ibm.wala.cast.python.PythonDriver;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;

/**
 * WALA Analysis Action
 */
class AnalysisAction extends AnAction {

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
		    		DocumentURLModule scriptModule = new DocumentURLModule(editor.getDocument());
		    		PythonDriver x = new PythonDriver(scriptModule);
		    		editor.getDocument().insertString(0, "" + offset + "\n" + x.getCallGraph("Lscript " + scriptModule.getName()) + "\n");
		    	} catch (IOException | ClassHierarchyException | java.lang.IllegalArgumentException | CancelException e) {
		    		editor.getDocument().insertString(0, e.toString());
		    	}
		    }
		    });
	    }
	    }, "analyze "  + FileDocumentManager.getInstance().getFile(editor.getDocument()).getName(), null);
    }
}
