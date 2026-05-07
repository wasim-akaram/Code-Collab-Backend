/*
 * Code reader note: Implements file/folder business logic, tree lookup, content updates, soft delete, restore, and search.
 */
package com.codesync.file.service;

import com.codesync.file.client.ProjectAccessClient;
import com.codesync.file.dto.CodeFileDto;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.exception.ResourceNotFoundException;
import com.codesync.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the FileService interface handling business logic.
 */
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final ProjectAccessClient projectAccessClient;

    private void assertCanEditProject(Long projectId, String userEmail, String authHeader) {
        if (!projectAccessClient.canEditProject(projectId, userEmail, authHeader)) {
            throw new RuntimeException("You do not have permission to edit this project");
        }
    }

    // ─── CREATE FILE ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CodeFileDto createFile(CodeFileDto dto, String userEmail, String authHeader) {
        assertCanEditProject(dto.getProjectId(), userEmail, authHeader);

        // Prevent duplicate paths in the same project
        // Path, not file name alone, is the unique workspace address.
        if (fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(dto.getProjectId(), dto.getPath())) {
            throw new RuntimeException("A file or folder already exists at path: " + dto.getPath());
        }

        // Auto-detect language from file extension
        String language = dto.getLanguage();
        if (language == null || language.isBlank()) {
            language = detectLanguage(dto.getName());
        }

        CodeFile file = CodeFile.builder()
                .projectId(dto.getProjectId())
                .name(dto.getName())
                .path(dto.getPath())
                .language(language)
                .content(dto.getContent() != null ? dto.getContent().replace("\u0000", "") : "")
                .size((long) (dto.getContent() != null ? dto.getContent().length() : 0))
                .isDirectory(dto.getIsDirectory() != null ? dto.getIsDirectory() : false)
                .createdBy(userEmail)
                .lastEditedBy(userEmail)
                .isDeleted(false)
                .build();

        return mapToDto(fileRepository.save(file));
    }

    // ─── CREATE FOLDER ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CodeFileDto createFolder(Long projectId, String path, String name, String userEmail, String authHeader) {
        assertCanEditProject(projectId, userEmail, authHeader);

        if (fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(projectId, path)) {
            throw new RuntimeException("A folder already exists at path: " + path);
        }

        CodeFile folder = CodeFile.builder()
                .projectId(projectId)
                .name(name)
                .path(path)
                .language(null)
                .content("")
                .size(0L)
                .isDirectory(true)
                .createdBy(userEmail)
                .lastEditedBy(userEmail)
                .isDeleted(false)
                .build();

        return mapToDto(fileRepository.save(folder));
    }

    // ─── GET FILE BY ID ─────────────────────────────────────────────────────────

    @Override
    public CodeFileDto getFileById(Long fileId) {
        CodeFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        return mapToDto(file);
    }

    // ─── GET FILE CONTENT ───────────────────────────────────────────────────────

    @Override
    public String getFileContent(Long fileId) {
        CodeFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        return file.getContent() != null ? file.getContent() : "";
    }

    // ─── GET FILES BY PROJECT (flat list) ───────────────────────────────────────

    @Override
    public List<CodeFileDto> getFilesByProject(Long projectId) {
        return fileRepository.findByProjectIdAndIsDeletedFalse(projectId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ─── GET FILE TREE (sorted for tree rendering) ──────────────────────────────

    @Override
    public List<CodeFileDto> getFileTree(Long projectId) {
        List<CodeFile> allFiles = fileRepository.findByProjectIdAndIsDeletedFalse(projectId);

        // Build a set of known directory paths so we can correctly classify
        // intermediate path segments as directories during sort-key construction.
        java.util.Set<String> dirPaths = allFiles.stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsDirectory()))
                .map(CodeFile::getPath)
                .map(p -> p.toLowerCase())
                .collect(java.util.stream.Collectors.toSet());

        return allFiles.stream()
                .sorted(Comparator.comparing(f -> treeSortKey(f, dirPaths)))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Builds a sort key that produces standard file-explorer tree ordering:
     * - At every directory level, folders sort before files
     * - Children appear immediately after their parent folder
     *
     * Each path segment is prefixed with "0:" for directory segments and
     * "1:" for file (leaf) segments, then lowercased for case-insensitive sort.
     *
     * Example keys:
     *   "ML Project" (dir)             → "0:ml project"
     *   "ML Project/Apple.java" (file) → "0:ml project/1:apple.java"
     *   "Main.java" (file)             → "1:main.java"
     *
     * Sorted order: ML Project → ML Project/Apple.java → Main.java  ✓
     */
    private String treeSortKey(CodeFile file, java.util.Set<String> dirPaths) {
        String path = file.getPath() != null ? file.getPath() : "";
        String[] parts = path.split("/");
        StringBuilder key = new StringBuilder();
        StringBuilder pathSoFar = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                key.append('/');
                pathSoFar.append('/');
            }
            pathSoFar.append(parts[i]);
            boolean isLastSegment = (i == parts.length - 1);

            if (isLastSegment && !Boolean.TRUE.equals(file.getIsDirectory())) {
                // Leaf file segment → "1:" so files sort after folders
                key.append("1:").append(parts[i].toLowerCase());
            } else {
                // Directory segment → "0:" so folders sort first
                key.append("0:").append(parts[i].toLowerCase());
            }
        }
        return key.toString();
    }

    // ─── UPDATE FILE CONTENT ────────────────────────────────────────────────────

    @Override
    @Transactional
    public CodeFileDto updateFileContent(Long fileId, String content, String userEmail, String authHeader) {
        CodeFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        assertCanEditProject(file.getProjectId(), userEmail, authHeader);

        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            throw new RuntimeException("Cannot set content on a directory");
        }

        file.setContent(content);
        file.setSize((long) (content != null ? content.length() : 0));
        file.setLastEditedBy(userEmail);

        return mapToDto(fileRepository.save(file));
    }

    // ─── RENAME FILE ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CodeFileDto renameFile(Long fileId, String newName, String userEmail, String authHeader) {
        CodeFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        assertCanEditProject(file.getProjectId(), userEmail, authHeader);

        String oldPath = file.getPath();
        // Keep the existing parent directory and replace only the final name.
        String parentDir = oldPath.contains("/")
                ? oldPath.substring(0, oldPath.lastIndexOf("/") + 1)
                : "";
        String newPath = parentDir + newName;

        // Check for duplicates
        if (fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(file.getProjectId(), newPath)) {
            throw new RuntimeException("A file already exists at path: " + newPath);
        }

        // If it's a directory, cascade rename to all children
        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            String oldPrefix = oldPath + "/";
            String newPrefix = newPath + "/";
            List<CodeFile> children = fileRepository.findByProjectIdAndPathStartingWith(
                    file.getProjectId(), oldPrefix);
            for (CodeFile child : children) {
                // Pattern.quote prevents regex characters in folder names from
                // being interpreted by replaceFirst.
                child.setPath(child.getPath().replaceFirst(
                        java.util.regex.Pattern.quote(oldPrefix), newPrefix));
            }
            fileRepository.saveAll(children);
        }

        file.setName(newName);
        file.setPath(newPath);
        file.setLastEditedBy(userEmail);

        return mapToDto(fileRepository.save(file));
    }

    // ─── MOVE FILE ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CodeFileDto moveFile(Long fileId, String newPath, String userEmail, String authHeader) {
        CodeFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        assertCanEditProject(file.getProjectId(), userEmail, authHeader);

        if (fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(file.getProjectId(), newPath)) {
            throw new RuntimeException("A file already exists at path: " + newPath);
        }

        // If directory, cascade move children
        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            String oldPrefix = file.getPath() + "/";
            String newPrefix = newPath + "/";
            List<CodeFile> children = fileRepository.findByProjectIdAndPathStartingWith(
                    file.getProjectId(), oldPrefix);
            for (CodeFile child : children) {
                // Children keep their relative path under the moved folder.
                child.setPath(child.getPath().replaceFirst(
                        java.util.regex.Pattern.quote(oldPrefix), newPrefix));
            }
            fileRepository.saveAll(children);
        }

        file.setPath(newPath);
        // Update name to last segment of new path
        file.setName(newPath.contains("/")
                ? newPath.substring(newPath.lastIndexOf("/") + 1)
                : newPath);
        file.setLastEditedBy(userEmail);

        return mapToDto(fileRepository.save(file));
    }

    // ─── DELETE FILE (soft) ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteFile(Long fileId, String userEmail, String authHeader) {
        CodeFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        assertCanEditProject(file.getProjectId(), userEmail, authHeader);

        file.setIsDeleted(true);

        // If it's a directory, cascade soft-delete to all children
        // Soft delete preserves rows for restore/history but hides them from
        // normal tree and search queries.
        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            List<CodeFile> children = fileRepository.findByProjectIdAndPathStartingWith(
                    file.getProjectId(), file.getPath() + "/");
            children.forEach(c -> c.setIsDeleted(true));
            fileRepository.saveAll(children);
        }

        fileRepository.save(file);
    }

    // ─── RESTORE FILE ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void restoreFile(Long fileId, String userEmail, String authHeader) {
        CodeFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        assertCanEditProject(file.getProjectId(), userEmail, authHeader);
        file.setIsDeleted(false);
        fileRepository.save(file);
    }

    // ─── SEARCH IN PROJECT ──────────────────────────────────────────────────────

    @Override
    public List<CodeFileDto> searchInProject(Long projectId, String query) {
        if (query == null || query.isBlank()) {
            return getFilesByProject(projectId);
        }
        return fileRepository.searchInProject(projectId, query).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ─── LANGUAGE DETECTION ─────────────────────────────────────────────────────

    private String detectLanguage(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "plaintext";
        // Language is inferred from the final extension only; unknown extensions
        // safely fall back to plaintext.
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (ext) {
            case "java" -> "java";
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "py" -> "python";
            case "c" -> "c";
            case "cpp", "cc", "cxx" -> "cpp";
            case "go" -> "go";
            case "rs" -> "rust";
            case "rb" -> "ruby";
            case "php" -> "php";
            case "kt" -> "kotlin";
            case "swift" -> "swift";
            case "r" -> "r";
            case "html" -> "html";
            case "css" -> "css";
            case "json" -> "json";
            case "xml" -> "xml";
            case "yaml", "yml" -> "yaml";
            case "md" -> "markdown";
            case "sql" -> "sql";
            case "sh", "bash" -> "shell";
            case "dockerfile" -> "dockerfile";
            default -> "plaintext";
        };
    }

    // ─── MAPPER ─────────────────────────────────────────────────────────────────

    private CodeFileDto mapToDto(CodeFile file) {
        return CodeFileDto.builder()
                .fileId(file.getFileId())
                .projectId(file.getProjectId())
                .name(file.getName())
                .path(file.getPath())
                .language(file.getLanguage())
                .content(file.getContent())
                .size(file.getSize())
                .isDirectory(file.getIsDirectory())
                .createdBy(file.getCreatedBy())
                .lastEditedBy(file.getLastEditedBy())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }
}
