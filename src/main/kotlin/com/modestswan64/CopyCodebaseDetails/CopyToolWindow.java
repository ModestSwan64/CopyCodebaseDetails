package com.modestswan64.CopyCodebaseDetails;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
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
    private final Project project;

    public CopyToolWindow(Project project) {
        super(true, true);
        this.project = project;
        setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 5, 5));

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

        ignoreFoldersField.setToolTipText("Comma separated folder names to ignore");

        JPanel ignorePanel = new JPanel();
        ignorePanel.setLayout(new BoxLayout(ignorePanel, BoxLayout.X_AXIS));
        ignorePanel.setBorder(JBUI.Borders.empty(5));

        JLabel ignoreLabel = new JLabel("Ignored Folders:");
        ignorePanel.add(ignoreLabel);
        ignorePanel.add(Box.createRigidArea(new Dimension(5, 0)));

        ignoreFoldersField.setMaximumSize(new Dimension(Integer.MAX_VALUE, JBUI.scale(22)));
        ignorePanel.add(ignoreFoldersField);

        clipboardPreview.setEditable(false);
        clipboardPreview.setLineWrap(true);
        clipboardPreview.setWrapStyleWord(true);
        JScrollPane previewScroll = new JScrollPane(clipboardPreview);

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.add(buttonPanel, BorderLayout.NORTH);
        topSection.add(ignorePanel, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(topSection, BorderLayout.NORTH);
        mainPanel.add(previewScroll, BorderLayout.CENTER);

        setContent(mainPanel);

        ignoreFoldersField.addActionListener(e -> {
            CopySettingsService service = ApplicationManager.getApplication().getService(CopySettingsService.class);
            service.setIgnoredFolders(getIgnoreFolders());
        });

        copyActiveFileBtn.addActionListener(e ->
                copyToClipboard(CopyUtils.collectActiveFile(project))
        );

        activeFilesFolderBtn.addActionListener(e -> {
            Set<String> ignores = getIgnoreFolders();
            ApplicationManager.getApplication().getService(CopySettingsService.class).setIgnoredFolders(ignores);
            copyToClipboard(CopyUtils.collectSiblingFiles(project));
        });

        copyEntireCodebaseBtn.addActionListener(e -> runInBackground(
                "Copying Entire Codebase",
                () -> CopyUtils.copyEntireCodebase(project, getIgnoreFolders())
        ));

        copyEntireCodebaseTreeBtn.addActionListener(e -> runInBackground(
                "Building Entire Codebase Tree",
                () -> CopyUtils.getCodebaseFolderTree(project, getIgnoreFolders())
        ));

        copySourcesRootTreeBtn.addActionListener(e -> runInBackground(
                "Building Source Root Tree",
                () -> CopyUtils.getSourceRootFolderTree(project, getIgnoreFolders())
        ));

        copySourcesRootCodebaseBtn.addActionListener(e -> runInBackground(
                "Copying Source Root Codebase",
                () -> CopyUtils.collectSourceRootFiles(project, getIgnoreFolders())
        ));
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

    private void runInBackground(String title, DataSupplier supplier) {
        new Task.Backgroundable(project, title, false) {
            @Override
            public void run(ProgressIndicator indicator) {
                Set<String> ignores = getIgnoreFolders();
                ApplicationManager.getApplication().getService(CopySettingsService.class).setIgnoredFolders(ignores);
                String result = supplier.get();

                ApplicationManager.getApplication().invokeLater(() -> copyToClipboard(result));
            }
        }.queue();
    }

    @FunctionalInterface
    private interface DataSupplier {
        String get();
    }
}
