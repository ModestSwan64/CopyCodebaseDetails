package com.modestswan64.CopyCodebaseDetails;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CopyUtils {

    private static final Map<String, String> COMMENT_PREFIX_MAP = Map.ofEntries(
            Map.entry("java", "// "), Map.entry("kt", "// "), Map.entry("py", "# "),
            Map.entry("js", "// "), Map.entry("ts", "// "), Map.entry("tsx", "// "),
            Map.entry("jsx", "// "), Map.entry("html", "<!-- "), Map.entry("xml", "<!-- "),
            Map.entry("css", "/* "), Map.entry("c", "// "), Map.entry("cpp", "// "),
            Map.entry("h", "// "), Map.entry("cs", "// "), Map.entry("rb", "# "),
            Map.entry("go", "// "), Map.entry("rs", "// "), Map.entry("sh", "# ")
    );

    public static String copyEntireCodebase(Project project, Set<String> ignoredFolders) {
        VirtualFile baseDir = VfsUtilCore.findRelativeFile(project.getBasePath(), null);
        if (baseDir == null) return "";

        StringBuilder result = new StringBuilder();
        ProjectFileIndex index = ProjectFileIndex.getInstance(project);
        FileTypeManager ftm = FileTypeManager.getInstance();

        VfsUtilCore.visitChildrenRecursively(baseDir, new VirtualFileVisitor<>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (!file.isDirectory()
                        && index.isInSource(file)
                        && !ftm.isFileIgnored(file)
                        && !isInIgnoredFolder(file, ignoredFolders)) {
                    appendFileContent(file, baseDir, result);
                }
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

    public static String collectSourceRootFiles(Project project, Set<String> ignoredFolders) {
        StringBuilder result = new StringBuilder();
        VirtualFile baseDir = VfsUtilCore.findRelativeFile(Objects.requireNonNull(project.getBasePath()), null);
        if (baseDir == null) return "";

        ProjectFileIndex index = ProjectFileIndex.getInstance(project);
        FileTypeManager ftm = FileTypeManager.getInstance();

        index.iterateContent(file -> {
            if (!file.isDirectory()
                    && isInSourceRoot(project, file)
                    && !ftm.isFileIgnored(file)
                    && !isInIgnoredFolder(file, ignoredFolders)) {
                appendFileContent(file, baseDir, result);
            }
            return true;
        });

        return result.toString();
    }

    public static String getCodebaseFolderTree(Project project, Set<String> ignoredFolders) {
        VirtualFile baseDir = VfsUtilCore.findRelativeFile(Objects.requireNonNull(project.getBasePath()), null);
        if (baseDir == null) return "";

        StringBuilder tree = new StringBuilder();
        buildFolderTree(baseDir, ignoredFolders, 0, tree);
        return tree.toString();
    }

    public static String getSourceRootFolderTree(Project project, Set<String> ignoredFolders) {
        VirtualFile sourceRoot = getSourceRoot(project);
        if (sourceRoot == null) return "";

        StringBuilder tree = new StringBuilder();
        buildFolderTree(sourceRoot, ignoredFolders, 0, tree);
        return tree.toString();
    }

    // --- Helper methods ---

    private static boolean isInIgnoredFolder(VirtualFile file, Set<String> ignoredFolders) {
        VirtualFile current = file;
        while (current != null) {
            if (ignoredFolders.contains(current.getName())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static boolean isInSourceRoot(Project project, VirtualFile file) {
        VirtualFile sourceRoot = getSourceRoot(project);
        if (sourceRoot == null) return false;
        return VfsUtilCore.isAncestor(sourceRoot, file, false);
    }

    private static VirtualFile getSourceRoot(Project project) {
        VirtualFile baseDir = VfsUtilCore.findRelativeFile(Objects.requireNonNull(project.getBasePath()), null);
        if (baseDir == null) return null;

        VirtualFile[] sourceRoots = ProjectRootManager.getInstance(project).getContentSourceRoots();
        for (VirtualFile root : sourceRoots) {
            if (VfsUtilCore.isAncestor(baseDir, root, false)) {
                return root;
            }
        }
        return null;
    }

    private static void buildFolderTree(VirtualFile folder, Set<String> ignoredFolders, int indent, StringBuilder tree) {
        if (!folder.isDirectory()) return;
        if (ignoredFolders.contains(folder.getName())) return;

        tree.append("  ".repeat(indent)).append("- ").append(folder.getName()).append("/\n");

        for (VirtualFile child : folder.getChildren()) {
            if (child.isDirectory()) {
                buildFolderTree(child, ignoredFolders, indent + 1, tree);
            } else {
                tree.append("  ".repeat(indent + 1)).append("-- ").append(child.getName()).append("\n");
            }
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
