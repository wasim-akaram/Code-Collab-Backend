package com.codesync.file.service;

import com.codesync.file.dto.CodeFileDto;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.exception.ResourceNotFoundException;
import com.codesync.file.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileServiceImpl fileService;

    private CodeFile mockFile;
    private CodeFileDto mockDto;
    private final String USER_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        mockFile = CodeFile.builder()
                .fileId(1L)
                .projectId(100L)
                .name("main.java")
                .path("src/main.java")
                .language("java")
                .isDirectory(false)
                .content("public class Main {}")
                .size(20L)
                .createdBy(USER_EMAIL)
                .lastEditedBy(USER_EMAIL)
                .isDeleted(false)
                .build();

        mockDto = CodeFileDto.builder()
                .projectId(100L)
                .name("main.java")
                .path("src/main.java")
                .isDirectory(false)
                .content("public class Main {}")
                .build();
    }

    // ─── createFile ─────────────────────────────────────────────────────────────

    @Test
    void createFile_shouldPersistAndReturnDto() {
        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(100L, "src/main.java")).thenReturn(false);
        when(fileRepository.save(any(CodeFile.class))).thenReturn(mockFile);

        CodeFileDto result = fileService.createFile(mockDto, USER_EMAIL, null);

        assertNotNull(result);
        assertEquals("src/main.java", result.getPath());
        assertEquals(USER_EMAIL, result.getCreatedBy());
        verify(fileRepository).save(any(CodeFile.class));
    }

    @Test
    void createFile_shouldThrowWhenDuplicatePath() {
        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(100L, "src/main.java")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.createFile(mockDto, USER_EMAIL, null));
        assertTrue(ex.getMessage().contains("already exists at path"));
        verify(fileRepository, never()).save(any());
    }

    @Test
    void createFile_shouldAutoDetectLanguageFromExtension() {
        mockDto = CodeFileDto.builder()
                .projectId(100L).name("script.py").path("script.py")
                .isDirectory(false).content("print('hello')").build();
        CodeFile pyFile = CodeFile.builder().fileId(2L).projectId(100L).name("script.py")
                .path("script.py").language("python").build();

        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(any(), any())).thenReturn(false);
        when(fileRepository.save(any(CodeFile.class))).thenAnswer(inv -> {
            CodeFile f = inv.getArgument(0);
            assertEquals("python", f.getLanguage());
            return pyFile;
        });

        fileService.createFile(mockDto, USER_EMAIL, null);
    }

    @Test
    void createFile_shouldUseProvidedLanguageOverAutoDetect() {
        mockDto = CodeFileDto.builder()
                .projectId(100L).name("file.js").path("file.js")
                .language("typescript") // explicit override
                .isDirectory(false).content("").build();

        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(any(), any())).thenReturn(false);
        when(fileRepository.save(any(CodeFile.class))).thenAnswer(inv -> {
            CodeFile f = inv.getArgument(0);
            assertEquals("typescript", f.getLanguage());
            return mockFile;
        });

        fileService.createFile(mockDto, USER_EMAIL, null);
    }

    @Test
    void createFile_shouldDefaultEmptyContentToEmptyString() {
        mockDto = CodeFileDto.builder().projectId(100L).name("x.java").path("x.java")
                .isDirectory(false).content(null).build();

        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(any(), any())).thenReturn(false);
        when(fileRepository.save(any(CodeFile.class))).thenAnswer(inv -> {
            CodeFile f = inv.getArgument(0);
            assertEquals("", f.getContent());
            return mockFile;
        });

        fileService.createFile(mockDto, USER_EMAIL, null);
    }

    // ─── createFolder ───────────────────────────────────────────────────────────

    @Test
    void createFolder_shouldPersistDirectoryNode() {
        CodeFile folder = CodeFile.builder().fileId(5L).projectId(100L).name("components")
                .path("src/components").isDirectory(true).createdBy(USER_EMAIL).build();
        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(100L, "src/components")).thenReturn(false);
        when(fileRepository.save(any(CodeFile.class))).thenReturn(folder);

        CodeFileDto result = fileService.createFolder(100L, "src/components", "components", USER_EMAIL, null);

        assertNotNull(result);
        assertTrue(result.getIsDirectory());
    }

    @Test
    void createFolder_shouldThrowWhenDuplicatePath() {
        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(100L, "src/components")).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> fileService.createFolder(100L, "src/components", "components", USER_EMAIL, null));
    }

    // ─── getFileById ────────────────────────────────────────────────────────────

    @Test
    void getFileById_shouldReturnDto() {
        when(fileRepository.findById(1L)).thenReturn(Optional.of(mockFile));

        CodeFileDto result = fileService.getFileById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getFileId());
        assertEquals("main.java", result.getName());
    }

    @Test
    void getFileById_shouldThrowWhenNotFound() {
        when(fileRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> fileService.getFileById(1L));
    }

    // ─── getFileContent ─────────────────────────────────────────────────────────

    @Test
    void getFileContent_shouldReturnContent() {
        when(fileRepository.findById(1L)).thenReturn(Optional.of(mockFile));

        String content = fileService.getFileContent(1L);
        assertEquals("public class Main {}", content);
    }

    @Test
    void getFileContent_shouldReturnEmptyStringWhenContentNull() {
        mockFile.setContent(null);
        when(fileRepository.findById(1L)).thenReturn(Optional.of(mockFile));

        assertEquals("", fileService.getFileContent(1L));
    }

    @Test
    void getFileContent_shouldThrowWhenNotFound() {
        when(fileRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> fileService.getFileContent(99L));
    }

    // ─── getFilesByProject ──────────────────────────────────────────────────────

    @Test
    void getFilesByProject_shouldReturnFiles() {
        when(fileRepository.findByProjectIdAndIsDeletedFalse(100L)).thenReturn(Arrays.asList(mockFile));

        List<CodeFileDto> result = fileService.getFilesByProject(100L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getProjectId());
    }

    @Test
    void getFilesByProject_shouldReturnEmptyListWhenNone() {
        when(fileRepository.findByProjectIdAndIsDeletedFalse(100L)).thenReturn(List.of());

        assertTrue(fileService.getFilesByProject(100L).isEmpty());
    }

    // ─── getFileTree ────────────────────────────────────────────────────────────

    @Test
    void getFileTree_shouldReturnDirectoriesFirst() {
        CodeFile dir = CodeFile.builder().fileId(2L).projectId(100L).name("src").path("src")
                .isDirectory(true).isDeleted(false).build();
        CodeFile file = CodeFile.builder().fileId(1L).projectId(100L).name("main.java")
                .path("src/main.java").isDirectory(false).isDeleted(false).build();

        when(fileRepository.findByProjectIdAndIsDeletedFalse(100L)).thenReturn(List.of(file, dir));

        List<CodeFileDto> tree = fileService.getFileTree(100L);

        assertEquals(2, tree.size());
        assertTrue(tree.get(0).getIsDirectory(), "Directories should come first");
        assertFalse(tree.get(1).getIsDirectory());
    }

    // ─── updateFileContent ──────────────────────────────────────────────────────

    @Test
    void updateFileContent_shouldUpdateAndReturn() {
        when(fileRepository.findById(1L)).thenReturn(Optional.of(mockFile));
        when(fileRepository.save(any(CodeFile.class))).thenReturn(mockFile);

        CodeFileDto result = fileService.updateFileContent(1L, "new content", USER_EMAIL, null);

        assertNotNull(result);
        assertEquals("new content", mockFile.getContent());
        assertEquals(11L, mockFile.getSize());
        assertEquals(USER_EMAIL, mockFile.getLastEditedBy());
        verify(fileRepository).save(mockFile);
    }

    @Test
    void updateFileContent_shouldThrowWhenNotFound() {
        when(fileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> fileService.updateFileContent(99L, "content", USER_EMAIL, null));
    }

    @Test
    void updateFileContent_shouldThrowWhenTargetIsDirectory() {
        mockFile.setIsDirectory(true);
        when(fileRepository.findById(1L)).thenReturn(Optional.of(mockFile));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.updateFileContent(1L, "content", USER_EMAIL, null));
        assertEquals("Cannot set content on a directory", ex.getMessage());
    }

    // ─── renameFile ─────────────────────────────────────────────────────────────

    @Test
    void renameFile_shouldRenameAndUpdatePath() {
        when(fileRepository.findById(1L)).thenReturn(Optional.of(mockFile));
        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(100L, "src/NewMain.java")).thenReturn(false);
        when(fileRepository.save(any(CodeFile.class))).thenReturn(mockFile);

        CodeFileDto result = fileService.renameFile(1L, "NewMain.java", USER_EMAIL, null);

        assertNotNull(result);
        assertEquals("NewMain.java", mockFile.getName());
        assertTrue(mockFile.getPath().endsWith("NewMain.java"));
    }

    @Test
    void renameFile_shouldThrowWhenDuplicatePath() {
        when(fileRepository.findById(1L)).thenReturn(Optional.of(mockFile));
        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(100L, "src/Conflict.java")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.renameFile(1L, "Conflict.java", USER_EMAIL, null));
        assertTrue(ex.getMessage().contains("already exists at path"));
    }

    @Test
    void renameFile_shouldThrowWhenNotFound() {
        when(fileRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> fileService.renameFile(99L, "new.java", USER_EMAIL, null));
    }

    // ─── moveFile ───────────────────────────────────────────────────────────────

    @Test
    void moveFile_shouldUpdatePath() {
        when(fileRepository.findById(1L)).thenReturn(Optional.of(mockFile));
        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(100L, "src/test/Main.java")).thenReturn(false);
        when(fileRepository.save(any(CodeFile.class))).thenReturn(mockFile);

        CodeFileDto result = fileService.moveFile(1L, "src/test/Main.java", USER_EMAIL, null);

        assertNotNull(result);
        assertEquals("src/test/Main.java", mockFile.getPath());
        assertEquals("Main.java", mockFile.getName());
    }

    @Test
    void moveFile_shouldThrowWhenDuplicatePath() {
        when(fileRepository.findById(1L)).thenReturn(Optional.of(mockFile));
        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(100L, "existing/path.java")).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> fileService.moveFile(1L, "existing/path.java", USER_EMAIL, null));
    }

    @Test
    void moveFile_shouldThrowWhenNotFound() {
        when(fileRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> fileService.moveFile(99L, "new/path.java", USER_EMAIL, null));
    }

    // ─── deleteFile ─────────────────────────────────────────────────────────────

    @Test
    void deleteFile_shouldSoftDelete() {
        when(fileRepository.findById(1L)).thenReturn(Optional.of(mockFile));
        when(fileRepository.save(any(CodeFile.class))).thenReturn(mockFile);

        fileService.deleteFile(1L, USER_EMAIL, null);

        assertTrue(mockFile.getIsDeleted());
        verify(fileRepository).save(mockFile);
    }

    @Test
    void deleteFile_shouldThrowWhenNotFound() {
        when(fileRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> fileService.deleteFile(99L, USER_EMAIL, null));
    }

    // ─── restoreFile ────────────────────────────────────────────────────────────

    @Test
    void restoreFile_shouldClearDeletedFlag() {
        mockFile.setIsDeleted(true);
        when(fileRepository.findById(1L)).thenReturn(Optional.of(mockFile));
        when(fileRepository.save(any(CodeFile.class))).thenReturn(mockFile);

        fileService.restoreFile(1L, USER_EMAIL, null);

        assertFalse(mockFile.getIsDeleted());
        verify(fileRepository).save(mockFile);
    }

    @Test
    void restoreFile_shouldThrowWhenNotFound() {
        when(fileRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> fileService.restoreFile(99L, USER_EMAIL, null));
    }

    // ─── searchInProject ────────────────────────────────────────────────────────

    @Test
    void searchInProject_shouldReturnMatchingFiles() {
        when(fileRepository.searchInProject(100L, "Main")).thenReturn(List.of(mockFile));

        List<CodeFileDto> results = fileService.searchInProject(100L, "Main");

        assertEquals(1, results.size());
        assertEquals("main.java", results.get(0).getName());
    }

    @Test
    void searchInProject_shouldReturnAllFilesWhenQueryBlank() {
        when(fileRepository.findByProjectIdAndIsDeletedFalse(100L)).thenReturn(List.of(mockFile));

        List<CodeFileDto> results = fileService.searchInProject(100L, "  ");

        assertEquals(1, results.size());
        verify(fileRepository, never()).searchInProject(any(), any());
    }

    @Test
    void searchInProject_shouldReturnAllFilesWhenQueryNull() {
        when(fileRepository.findByProjectIdAndIsDeletedFalse(100L)).thenReturn(List.of(mockFile));

        List<CodeFileDto> results = fileService.searchInProject(100L, null);
        assertEquals(1, results.size());
    }
}
