package com.modestswan64.CopyCodebaseDetails;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CopyUtils {

    private static final Map<String, String> COMMENT_PREFIX_MAP = new HashMap<>();

    static {
        COMMENT_PREFIX_MAP.put("java", "// ");
        COMMENT_PREFIX_MAP.put("kt", "// ");
        COMMENT_PREFIX_MAP.put("py", "# ");
        COMMENT_PREFIX_MAP.put("js", "// ");
        COMMENT_PREFIX_MAP.put("ts", "// ");
        COMMENT_PREFIX_MAP.put("tsx", "// ");
        COMMENT_PREFIX_MAP.put("jsx", "// ");
        COMMENT_PREFIX_MAP.put("html", "<!-- ");
        COMMENT_PREFIX_MAP.put("xml", "<!-- ");
        COMMENT_PREFIX_MAP.put("css", "/* ");
        COMMENT_PREFIX_MAP.put("c", "// ");
        COMMENT_PREFIX_MAP.put("cpp", "// ");
        COMMENT_PREFIX_MAP.put("h", "// ");
        COMMENT_PREFIX_MAP.put("cs", "// ");
        COMMENT_PREFIX_MAP.put("rb", "# ");
        COMMENT_PREFIX_MAP.put("go", "// ");
        COMMENT_PREFIX_MAP.put("rs", "// ");
        COMMENT_PREFIX_MAP.put("sh", "# ");
    }

    public static String copyEntireCodebase(Project project) {

        VirtualFile baseDir = project.getBasePath() != null
                ? VfsUtilCore.findRelativeFile(project.getBasePath(), null)
                : null;

        if (baseDir == null) {
            Messages.showErrorDialog(project, "Project base directory not found.", "Copy Failed");
            return "";
        }

        StringBuilder result = new StringBuilder();

        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        FileTypeManager fileTypeManager = FileTypeManager.getInstance();

        VfsUtilCore.visitChildrenRecursively(baseDir, new VirtualFileVisitor<>() {
            @Override
            public boolean visitFile(VirtualFile file) {
                if (!file.isDirectory()
                        && fileIndex.isInSource(file)
                        && !fileTypeManager.isFileIgnored(file)) {
                    try {
                        String relativePath = VfsUtilCore.getRelativePath(file, baseDir, '/');
                        if (relativePath != null) {
                            String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);

                            String comment = getCommentSyntax(file.getExtension(), relativePath);

                            result.append(comment)
                                    .append("\n")
                                    .append(content)
                                    .append("\n\n");
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                return true;
            }
        });
        return result.toString();
    }

    public static String collectActiveFile(Project project) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        Editor editor = fileEditorManager.getSelectedTextEditor();

        if (editor == null) return "";

        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (file == null) return "";

        try {
            String basePath = project.getBasePath();
            String relativePath = VfsUtilCore.getRelativePath(file, VfsUtilCore.findRelativeFile(basePath, null), '/');

            String comment = getCommentSyntax(file.getExtension(), relativePath);
            String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);

            return comment + "\n" + content;

        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String collectSiblingFiles(Project project) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        Editor editor = fileEditorManager.getSelectedTextEditor();

        if (editor == null) return "";

        VirtualFile currentFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (currentFile == null) return "";

        VirtualFile parentDir = currentFile.getParent();
        if (parentDir == null) return "";

        StringBuilder result = new StringBuilder();
        String basePath = project.getBasePath();
        VirtualFile baseDir = basePath != null ? VfsUtilCore.findRelativeFile(basePath, null) : null;

        for (VirtualFile file : parentDir.getChildren()) {
            if (!file.isDirectory()) {
                try {
                    String relativePath = VfsUtilCore.getRelativePath(file, baseDir, '/');
                    String comment = getCommentSyntax(file.getExtension(), relativePath);
                    String content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);

                    result.append(comment).append("\n").append(content).append("\n\n");

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result.toString();
    }

    public static String getCodebaseFolderTree(Project project) {
        String basePath = project.getBasePath();
        if (basePath == null) return "";

        VirtualFile baseDir = VfsUtilCore.findRelativeFile(basePath, null);
        if (baseDir == null) return "";

        StringBuilder tree = new StringBuilder();

        VfsUtilCore.visitChildrenRecursively(baseDir, new VirtualFileVisitor<Object>() {
            private int depth = 0;

            @Override
            public boolean visitFile(VirtualFile file) {
                String indent = "  ".repeat(depth);
                if (file.isDirectory()) {
                    tree.append(indent).append("- ").append(file.getName()).append("/\n");
                    depth++;
                } else {
                    tree.append(indent).append("-- ").append(file.getName()).append("\n");
                }
                return true;
            }

            @Override
            public void afterChildrenVisited(VirtualFile file) {
                if (file.isDirectory()) {
                    depth--;
                }
            }
        });

        return tree.toString();
    }

    private static String getCommentSyntax(String extension, String path) {
        if (extension == null) {
            return "// " + path;
        }
        String prefix = COMMENT_PREFIX_MAP.getOrDefault(extension.toLowerCase(), "// ");
        if (prefix.equals("<!-- ")) {
            return prefix + path + " -->";
        } else if (prefix.equals("/* ")) {
            return prefix + path + " */";
        }
        return prefix + path;
    }
}