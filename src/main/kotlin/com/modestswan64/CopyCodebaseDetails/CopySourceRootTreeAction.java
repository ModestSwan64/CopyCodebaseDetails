package com.modestswan64.CopyCodebaseDetails;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.awt.datatransfer.StringSelection;
import java.util.Set;

public class CopySourceRootTreeAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        Set<String> ignores = ApplicationManager.getApplication()
                .getService(CopySettingsService.class)
                .getIgnoredFolders();
        String result = CopyUtils.getSourceRootFolderTree(project, ignores);
        CopyPasteManager.getInstance().setContents(new StringSelection(result));
        Messages.showInfoMessage("Source root folder tree copied to clipboard.", "Success");
    }
}
