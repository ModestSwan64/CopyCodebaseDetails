package com.modestswan64.CopyCodebaseDetails;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.awt.datatransfer.StringSelection;

public class CopyActiveFileAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        String result = CopyUtils.collectActiveFile(project);
        CopyPasteManager.getInstance().setContents(new StringSelection(result));
        Messages.showInfoMessage("Active file copied to clipboard.", "Success");
    }
}