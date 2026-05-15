/*
 * Code reader note: Defines workspace file and folder operations implemented by the service layer.
 */
package com.codesync.file.service;

import com.codesync.file.dto.CodeFileDto;
import java.util.List;

/**
 * Interface defining the business logic operations for files.
 * Matches the FileService contract from the documentation (§4.3).
 */
public interface FileService {

    /** Creates a new file in a project. */
    CodeFileDto createFile(CodeFileDto fileDto, String userEmail, String authHeader);

    /** Creates a new folder in a project. */
    CodeFileDto createFolder(Long projectId, String path, String name, String userEmail, String authHeader);

    /** Retrieves a file by its unique ID. */
    CodeFileDto getFileById(Long fileId);

    /** Retrieves the raw text content of a file. */
    String getFileContent(Long fileId);

    /** Retrieves all active files/folders for a project (flat list sorted for tree). */
    List<CodeFileDto> getFilesByProject(Long projectId);

    /** Retrieves the file tree for a project (same as getFilesByProject but sorted). */
    List<CodeFileDto> getFileTree(Long projectId);

    /** Updates the text content of a specific file. */
    CodeFileDto updateFileContent(Long fileId, String content, String userEmail, String authHeader);

    /** Renames a file and updates its path. */
    CodeFileDto renameFile(Long fileId, String newName, String userEmail, String authHeader);

    /** Moves a file to a new path. */
    CodeFileDto moveFile(Long fileId, String newPath, String userEmail, String authHeader);

    /** Soft-deletes a file (or folder and its children). */
    void deleteFile(Long fileId, String userEmail, String authHeader);

    /** Restores a soft-deleted file. */
    void restoreFile(Long fileId, String userEmail, String authHeader);

    /** Search within a project by filename or content. */
    List<CodeFileDto> searchInProject(Long projectId, String query);
}
