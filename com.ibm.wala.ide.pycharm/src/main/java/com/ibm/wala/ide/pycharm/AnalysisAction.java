package com.ibm.wala.ide.pycharm;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.python.core.PyObject;

import com.ibm.wala.cast.tree.impl.CAstTypeDictionaryImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

/**
 * Implode / Explode Action
 */
class AnalysisAction extends AnAction {

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
    	CAstTypeDictionaryImpl<PyObject> dict = new CAstTypeDictionaryImpl<PyObject>();
	CommandProcessor.getInstance().executeCommand(project, new java.lang.Runnable() {
	    public void run() {
		ApplicationManager.getApplication().runWriteAction(new java.lang.Runnable() {
		    public void run() {
		    	try {
		    		PythonDocumentParser parser = new PythonDocumentParser(editor.getDocument(), dict);
		    		PyObject	 x = parser.parse();
		    		editor.getDocument().insertString(0, x.toString());
		    	} catch (IOException e) {
		    		editor.getDocument().insertString(0, e.toString());
		    	}
		    }
		    });
	    }
	    }, "bad", null);
    }
}
