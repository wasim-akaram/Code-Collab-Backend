/*
 * Code reader note: Exposes REST endpoints for file/folder creation, upload, lookup, content updates, search, move, rename, delete, and restore.
 */
package com.codesync.file.controller;

import com.codesync.file.dto.CodeFileDto;
import com.codesync.file.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST controller for file operations.
 *
 * All mutating endpoints require the X-User header (set by the API Gateway
 * from the JWT subject, which is the user's email).
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    // ─── CREATE ─────────────────────────────────────────────────────────────────

    /** Creates a new file in a project. */
    @PostMapping
    public ResponseEntity<CodeFileDto> createFile(@Valid @RequestBody CodeFileDto fileDto,
                                                  @RequestHeader("X-User") String userEmail,
                                                  @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return new ResponseEntity<>(fileService.createFile(fileDto, userEmail, authHeader), HttpStatus.CREATED);
    }

    /** Creates a new folder in a project. */
    @PostMapping("/folder")
    public ResponseEntity<CodeFileDto> createFolder(@RequestBody Map<String, Object> body,
                                                    @RequestHeader("X-User") String userEmail,
                                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long projectId = Long.valueOf(body.get("projectId").toString());
        String path = body.get("path").toString();
        String name = body.get("name").toString();
        return new ResponseEntity<>(fileService.createFolder(projectId, path, name, userEmail, authHeader),
                                    HttpStatus.CREATED);
    }

    /**
     * Upload a local file into a project.
     * POST /files/upload
     * Form fields: projectId (Long), path (String, e.g. "src/"), file (binary).
     */
    @PostMapping("/upload")
    public ResponseEntity<CodeFileDto> uploadFile(
            @RequestParam("projectId") Long projectId,
            @RequestParam(value = "path", defaultValue = "") String path,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User") String userEmail,
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws IOException {

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : file.getName();
        // Build a simple text file DTO — read bytes as UTF-8 text.
        String content;
        try {
            content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            // PostgreSQL TEXT columns cannot store null bytes (0x00).
            // Binary files decoded as UTF-8 often contain these.
            content = content.replace("\u0000", "");
        } catch (Exception e) {
            content = ""; // binary file — store without content
        }

        // Normalise path: strip trailing slash so join is clean
        String dir = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        // Strip leading slash — regular file creation uses bare paths like "ML Project/Apple.java"
        while (dir.startsWith("/")) {
            dir = dir.substring(1);
        }
        // Build FULL file path: directory/filename (consistent with regular file creation)
        String filePath = dir.isBlank() ? filename : (dir + "/" + filename);

        CodeFileDto dto = new CodeFileDto();
        dto.setProjectId(projectId);
        dto.setName(filename);
        dto.setPath(filePath);
        dto.setContent(content);
        dto.setIsDirectory(false);

        return new ResponseEntity<>(fileService.createFile(dto, userEmail, authHeader), HttpStatus.CREATED);
    }

    // ─── READ ───────────────────────────────────────────────────────────────────

    /** Retrieves a specific file by its ID (full details including content). */
    @GetMapping("/{fileId}")
    public ResponseEntity<CodeFileDto> getFileById(@PathVariable Long fileId) {
        return ResponseEntity.ok(fileService.getFileById(fileId));
    }

    /** Retrieves just the raw text content of a file. */
    @GetMapping("/{fileId}/content")
    public ResponseEntity<String> getFileContent(@PathVariable Long fileId) {
        return ResponseEntity.ok(fileService.getFileContent(fileId));
    }

    /** Retrieves all active files/folders for a given project ID. */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<CodeFileDto>> getFilesByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(fileService.getFilesByProject(projectId));
    }

    /** Retrieves the file tree for a project (sorted: dirs first, then alphabetical). */
    @GetMapping("/project/{projectId}/tree")
    public ResponseEntity<List<CodeFileDto>> getFileTree(@PathVariable Long projectId) {
        return ResponseEntity.ok(fileService.getFileTree(projectId));
    }

    /** Search within a project by filename or content. */
    @GetMapping("/project/{projectId}/search")
    public ResponseEntity<List<CodeFileDto>> searchInProject(@PathVariable Long projectId,
                                                              @RequestParam String q) {
        return ResponseEntity.ok(fileService.searchInProject(projectId, q));
    }

    // ─── UPDATE ─────────────────────────────────────────────────────────────────

    /** Updates the text content of a file. */
    @PutMapping("/{fileId}/content")
    public ResponseEntity<CodeFileDto> updateFileContent(@PathVariable Long fileId,
                                                         @RequestBody Map<String, String> body,
                                                         @RequestHeader("X-User") String userEmail,
                                                         @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String content = body.get("content");
        return ResponseEntity.ok(fileService.updateFileContent(fileId, content, userEmail, authHeader));
    }

    /** Renames a file or folder. */
    @PutMapping("/{fileId}/rename")
    public ResponseEntity<CodeFileDto> renameFile(@PathVariable Long fileId,
                                                  @RequestBody Map<String, String> body,
                                                  @RequestHeader("X-User") String userEmail,
                                                  @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String newName = body.get("newName");
        return ResponseEntity.ok(fileService.renameFile(fileId, newName, userEmail, authHeader));
    }

    /** Moves a file or folder to a new path. */
    @PutMapping("/{fileId}/move")
    public ResponseEntity<CodeFileDto> moveFile(@PathVariable Long fileId,
                                                @RequestBody Map<String, String> body,
                                                @RequestHeader("X-User") String userEmail,
                                                @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String newPath = body.get("newPath");
        return ResponseEntity.ok(fileService.moveFile(fileId, newPath, userEmail, authHeader));
    }

    // ─── DELETE / RESTORE ───────────────────────────────────────────────────────

    /** Soft-deletes a file (or folder and its children). */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long fileId,
                                           @RequestHeader("X-User") String userEmail,
                                           @RequestHeader(value = "Authorization", required = false) String authHeader) {
        fileService.deleteFile(fileId, userEmail, authHeader);
        return ResponseEntity.noContent().build();
    }

    /** Restores a soft-deleted file. */
    @PostMapping("/{fileId}/restore")
    public ResponseEntity<Void> restoreFile(@PathVariable Long fileId,
                                            @RequestHeader("X-User") String userEmail,
                                            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        fileService.restoreFile(fileId, userEmail, authHeader);
        return ResponseEntity.ok().build();
    }
}
