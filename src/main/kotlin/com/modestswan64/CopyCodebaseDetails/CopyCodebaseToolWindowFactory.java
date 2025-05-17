package com.modestswan64.CopyCodebaseDetails;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;

public class CopyCodebaseToolWindowFactory implements ToolWindowFactory {

    private JTextArea textArea;

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 5, 5));

        JButton copyCodebaseBtn = new JButton("Copy Codebase");
        JButton copyActiveFileBtn = new JButton("Copy Active File");
        JButton copyFolderFilesBtn = new JButton("Copy Folder's Files");
        JButton copyTreeBtn = new JButton("Copy Tree");

        ActionListener updateClipboardAndTextArea = e -> {
            String text = switch (e.getActionCommand()) {
                case "Copy Codebase" -> CopyUtils.copyEntireCodebase(project);
                case "Copy Active File" -> CopyUtils.collectActiveFile(project);
                case "Copy Folder's Files" -> CopyUtils.collectSiblingFiles(project);
                case "Copy Tree" -> CopyUtils.getCodebaseFolderTree(project);
                default -> "";
            };
            CopyPasteManager.getInstance().setContents(new StringSelection(text));
            textArea.setText(text);
        };

        copyCodebaseBtn.setActionCommand("Copy Codebase");
        copyActiveFileBtn.setActionCommand("Copy Active File");
        copyFolderFilesBtn.setActionCommand("Copy Folder's Files");
        copyTreeBtn.setActionCommand("Copy Tree");

        copyCodebaseBtn.addActionListener(updateClipboardAndTextArea);
        copyActiveFileBtn.addActionListener(updateClipboardAndTextArea);
        copyFolderFilesBtn.addActionListener(updateClipboardAndTextArea);
        copyTreeBtn.addActionListener(updateClipboardAndTextArea);

        buttonPanel.add(copyCodebaseBtn);
        buttonPanel.add(copyActiveFileBtn);
        buttonPanel.add(copyFolderFilesBtn);
        buttonPanel.add(copyTreeBtn);

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JBScrollPane(textArea);

        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        Content content = ContentFactory.getInstance().createContent(mainPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}