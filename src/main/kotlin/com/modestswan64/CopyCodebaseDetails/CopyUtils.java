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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class CopyUtils {

    private static final Map<String, String> COMMENT_PREFIX_MAP = Map.ofEntries(
            Map.entry("java", "// "), Map.entry("kt", "// "), Map.entry("py", "# "),
            Map.entry("js", "// "), Map.entry("ts", "// "), Map.entry("tsx", "// "),
            Map.entry("jsx", "// "), Map.entry("html", "<!-- "), Map.entry("xml", "<!-- "),
            Map.entry("css", "/* "), Map.entry("c", "// "), Map.entry("cpp", "// "),
            Map.entry("h", "// "), Map.entry("cs", "// "), Map.entry("rb", "# "),
            Map.entry("go", "// "), Map.entry("rs", "// "), Map.entry("sh", "# ")
    );

    public static String copyEntireCodebase(Project project) {
        VirtualFile baseDir = VfsUtilCore.findRelativeFile(Objects.requireNonNull(project.getBasePath()), null);
        if (baseDir == null) {
            Messages.showErrorDialog(project, "Project base directory not found.", "Copy Failed");
            return "";
        }

        StringBuilder result = new StringBuilder();
        ProjectFileIndex index = ProjectFileIndex.getInstance(project);
        FileTypeManager ftm = FileTypeManager.getInstance();

        VfsUtilCore.visitChildrenRecursively(baseDir, new VirtualFileVisitor<>() {
            public boolean visitFile(@NotNull VirtualFile file) {
                if (!file.isDirectory() && index.isInSource(file) && !ftm.isFileIgnored(file))
                    appendFileContent(file, baseDir, result);
                return true;
            }
        });

        return result.toString();
    }

    public static String collectActiveFile(Project project) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) return "";
        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        return file == null ? "" : readFileWithComment(file, project);
    }

    public static String collectSiblingFiles(Project project) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) return "";
        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (file == null || file.getParent() == null) return "";

        StringBuilder result = new StringBuilder();
        VirtualFile baseDir = VfsUtilCore.findRelativeFile(Objects.requireNonNull(project.getBasePath()), null);

        for (VirtualFile f : file.getParent().getChildren())
            if (!f.isDirectory()) appendFileContent(f, baseDir, result);

        return result.toString();
    }

    public static String collectSourceRootFiles(Project project) {
        VirtualFile baseDir = VfsUtilCore.findRelativeFile(Objects.requireNonNull(project.getBasePath()), null);
        if (baseDir == null) return "";

        StringBuilder result = new StringBuilder();
        FileTypeManager ftm = FileTypeManager.getInstance();

        VfsUtilCore.visitChildrenRecursively(baseDir, new VirtualFileVisitor<>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (!file.isDirectory() && ProjectFileIndex.getInstance(project).isInSource(file) && !ftm.isFileIgnored(file)) {
                    appendFileContent(file, baseDir, result);
                }
                return true;
            }
        });

        return result.toString();
    }

    public static String getCodebaseFolderTree(Project project) {
        VirtualFile baseDir = VfsUtilCore.findRelativeFile(Objects.requireNonNull(project.getBasePath()), null);
        if (baseDir == null) return "";

        StringBuilder tree = new StringBuilder();
        VfsUtilCore.visitChildrenRecursively(baseDir, new VirtualFileVisitor<>() {
            int depth = 0;

            public boolean visitFile(@NotNull VirtualFile file) {
                tree.append("  ".repeat(depth))
                        .append(file.isDirectory() ? "- " : "-- ")
                        .append(file.getName())
                        .append(file.isDirectory() ? "/\n" : "\n");
                if (file.isDirectory()) depth++;
                return true;
            }

            public void afterChildrenVisited(@NotNull VirtualFile file) {
                if (file.isDirectory()) depth--;
            }
        });

        return tree.toString();
    }

    public static String getSourceRootFolderTree(Project project) {
        StringBuilder result = new StringBuilder();
        ProjectFileIndex.getInstance(project).iterateContent(file -> {
            if (!file.isDirectory()) {
                VirtualFile root = ProjectFileIndex.getInstance(project).getSourceRootForFile(file);
                if (root != null) {
                    result.setLength(0);
                    buildTree(root, result, 0);
                    return false;
                }
            }
            return true;
        });
        return result.toString();
    }

    private static void buildTree(VirtualFile dir, StringBuilder builder, int depth) {
        builder.append("  ".repeat(depth)).append("- ").append(dir.getName()).append("/\n");
        for (VirtualFile child : dir.getChildren()) {
            if (child.isDirectory()) buildTree(child, builder, depth + 1);
            else builder.append("  ".repeat(depth + 1)).append("-- ").append(child.getName()).append("\n");
        }
    }

    private static void appendFileContent(VirtualFile file, VirtualFile baseDir, StringBuilder result) {
        try {
            String relPath = VfsUtilCore.getRelativePath(file, baseDir, '/');
            if (relPath != null) {
                result.append(getCommentSyntax(file.getExtension(), relPath))
                        .append("\n")
                        .append(new String(file.contentsToByteArray(), StandardCharsets.UTF_8))
                        .append("\n\n");
            }
        } catch (IOException ignored) {
        }
    }

    private static String readFileWithComment(VirtualFile file, Project project) {
        try {
            String relPath = VfsUtilCore.getRelativePath(file, Objects.requireNonNull(VfsUtilCore.findRelativeFile(Objects.requireNonNull(project.getBasePath()), null)), '/');
            return getCommentSyntax(file.getExtension(), relPath) + "\n" +
                    new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static String getCommentSyntax(String ext, String path) {
        if (ext == null) return "// " + path;
        String prefix = COMMENT_PREFIX_MAP.getOrDefault(ext.toLowerCase(), "// ");
        return switch (prefix) {
            case "<!-- " -> prefix + path + " -->";
            case "/* " -> prefix + path + " */";
            default -> prefix + path;
        };
    }
}
