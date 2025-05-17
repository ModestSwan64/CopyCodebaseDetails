package com.modestswan64.CopyCodebaseDetails;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CopyToolWindow extends SimpleToolWindowPanel {

    private final JTextArea clipboardPreview = new JTextArea();
    private final JTextField ignoreFoldersField = new JTextField();

    public CopyToolWindow(Project project) {
        super(true, true);
        setLayout(new BorderLayout());

        // Create the top section with buttons
        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 5, 5)); // 2 rows, 3 cols

        JButton copyActiveFileBtn = new JButton("Active File");
        JButton activeFilesFolderBtn = new JButton("Active File's Folder");
        JButton copyEntireCodebaseBtn = new JButton("Entire Codebase");
        JButton copyEntireCodebaseTreeBtn = new JButton("Entire Codebase's Tree");
        JButton copySourcesRootTreeBtn = new JButton("Sources Root Tree");
        JButton copySourcesRootCodebaseBtn = new JButton("Sources Root Codebase");

        buttonPanel.add(copyActiveFileBtn);
        buttonPanel.add(activeFilesFolderBtn);
        buttonPanel.add(copyEntireCodebaseBtn);
        buttonPanel.add(copyEntireCodebaseTreeBtn);
        buttonPanel.add(copySourcesRootTreeBtn);
        buttonPanel.add(copySourcesRootCodebaseBtn);

        // Ignore folders field setup with fixed height
        ignoreFoldersField.setToolTipText("Comma separated folder names to ignore");

        // Create a fixed size text field with constrained height
        JPanel ignorePanel = new JPanel();
        ignorePanel.setLayout(new BoxLayout(ignorePanel, BoxLayout.X_AXIS));
        ignorePanel.setBorder(JBUI.Borders.empty(5));

        // Add label for ignored folders
        JLabel ignoreLabel = new JLabel("Ignored Folders:");
        ignorePanel.add(ignoreLabel);
        ignorePanel.add(Box.createRigidArea(new Dimension(5, 0))); // spacing

        // Set text field to have a fixed height but fill width
        ignoreFoldersField.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(22)));
        ignorePanel.add(ignoreFoldersField);

        // Clipboard preview setup
        clipboardPreview.setEditable(false);
        clipboardPreview.setLineWrap(true);
        clipboardPreview.setWrapStyleWord(true);
        JScrollPane previewScroll = new JScrollPane(clipboardPreview);

        // Create the content panel with proper North/Center layout
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.add(buttonPanel, BorderLayout.NORTH);
        topSection.add(ignorePanel, BorderLayout.CENTER);

        // Main panel layout - top section is NORTH, preview is CENTER (will expand)
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topSection, BorderLayout.NORTH);
        mainPanel.add(previewScroll, BorderLayout.CENTER);

        setContent(mainPanel);

        // Save ignores when Enter is pressed
        ignoreFoldersField.addActionListener(e -> {
            CopySettingsService service = ApplicationManager.getApplication().getService(CopySettingsService.class);
            service.setIgnoredFolders(getIgnoreFolders());
        });

        // Copy actions
        copyActiveFileBtn.addActionListener(e ->
                copyToClipboard(CopyUtils.collectActiveFile(project))
        );

        activeFilesFolderBtn.addActionListener(e -> {
            Set<String> ignores = getIgnoreFolders();
            ApplicationManager.getApplication().getService(CopySettingsService.class).setIgnoredFolders(ignores);
            copyToClipboard(CopyUtils.collectSiblingFiles(project));
        });

        copyEntireCodebaseBtn.addActionListener(e -> {
            Set<String> ignores = getIgnoreFolders();
            ApplicationManager.getApplication().getService(CopySettingsService.class).setIgnoredFolders(ignores);
            copyToClipboard(CopyUtils.copyEntireCodebase(project, ignores));
        });

        copyEntireCodebaseTreeBtn.addActionListener(e -> {
            Set<String> ignores = getIgnoreFolders();
            ApplicationManager.getApplication().getService(CopySettingsService.class).setIgnoredFolders(ignores);
            copyToClipboard(CopyUtils.getCodebaseFolderTree(project, ignores));
        });

        copySourcesRootTreeBtn.addActionListener(e -> {
            Set<String> ignores = getIgnoreFolders();
            ApplicationManager.getApplication().getService(CopySettingsService.class).setIgnoredFolders(ignores);
            copyToClipboard(CopyUtils.getSourceRootFolderTree(project, ignores));
        });

        copySourcesRootCodebaseBtn.addActionListener(e -> {
            Set<String> ignores = getIgnoreFolders();
            ApplicationManager.getApplication().getService(CopySettingsService.class).setIgnoredFolders(ignores);
            copyToClipboard(CopyUtils.collectSourceRootFiles(project, ignores));
        });
    }

    private Set<String> getIgnoreFolders() {
        String text = ignoreFoldersField.getText().trim();
        if (text.isEmpty()) return Collections.emptySet();
        String[] parts = text.split("\\s*,\\s*");
        return new HashSet<>(Arrays.asList(parts));
    }

    private void copyToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        clipboardPreview.setText(text);
    }
}