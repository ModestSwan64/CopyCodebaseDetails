package com.modestswan64.CopyCodebaseDetails;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class CopyToolWindow extends SimpleToolWindowPanel {

    private final JTextArea clipboardPreview = new JTextArea();

    public CopyToolWindow(Project project) {
        super(true, true);
        setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 5, 5)); // 2 rows, 3 columns, spacing 5px

        JButton copyActiveFileBtn = new JButton("Active File");
        JButton activeFilesFolderBtn = new JButton("Active File's Folder");
        JButton copyEntireCodebaseBtn = new JButton("Entire Codebase");
        JButton copyEntireCodebaseTreeBtn = new JButton("Entire Codebase's Tree");
        JButton copySourcesRootTreeBtn = new JButton("Sources Root Tree");
        JButton copySourcesRootCodebaseBtn = new JButton("Sources Root Codebase");


        copyActiveFileBtn.addActionListener(e -> copyToClipboard(CopyUtils.collectActiveFile(project)));
        activeFilesFolderBtn.addActionListener(e -> copyToClipboard(CopyUtils.collectSiblingFiles(project)));
        copyEntireCodebaseBtn.addActionListener(e -> copyToClipboard(CopyUtils.copyEntireCodebase(project)));
        copyEntireCodebaseTreeBtn.addActionListener(e -> copyToClipboard(CopyUtils.getCodebaseFolderTree(project)));
        copySourcesRootTreeBtn.addActionListener(e -> copyToClipboard(CopyUtils.getSourceRootFolderTree(project)));
        copySourcesRootCodebaseBtn.addActionListener(e -> copyToClipboard(CopyUtils.collectSourceRootFiles(project)));

        buttonPanel.add(copyActiveFileBtn);
        buttonPanel.add(activeFilesFolderBtn);
        buttonPanel.add(copyEntireCodebaseBtn);
        buttonPanel.add(copyEntireCodebaseTreeBtn);
        buttonPanel.add(copySourcesRootTreeBtn);
        buttonPanel.add(copySourcesRootCodebaseBtn);

        clipboardPreview.setEditable(false);
        clipboardPreview.setLineWrap(true);
        clipboardPreview.setWrapStyleWord(true);

        JScrollPane previewScroll = new JScrollPane(clipboardPreview);

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(previewScroll, BorderLayout.CENTER);

        setContent(mainPanel);
    }

    private void copyToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        clipboardPreview.setText(text);
    }
}
