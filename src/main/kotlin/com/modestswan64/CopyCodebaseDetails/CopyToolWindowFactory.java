package com.modestswan64.CopyCodebaseDetails;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class CopyToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CopyToolWindow copyToolWindow = new CopyToolWindow(project);
        Content content = ContentFactory.getInstance().createContent(copyToolWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
