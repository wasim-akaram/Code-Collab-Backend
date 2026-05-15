package com.codesync.file.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codesync.file.client.ProjectAccessClient;
import com.codesync.file.dto.CodeFileDto;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.exception.ResourceNotFoundException;
import com.codesync.file.repository.FileRepository;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private ProjectAccessClient projectAccessClient;

    @InjectMocks
    private FileServiceImpl fileService;

    private static final String USER = "test@test.com";
    private static final String AUTH = "Bearer token";

    @BeforeEach
    void setUp() {
        lenient().when(projectAccessClient.canEditProject(anyLong(), anyString(), anyString())).thenReturn(true);
    }

    @Test
    void createFile_Success() {
        CodeFileDto dto = new CodeFileDto();
        dto.setProjectId(1L);
        dto.setPath("test.java");
        dto.setName("test.java");
        dto.setContent("public class Test {}");

        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(1L, "test.java")).thenReturn(false);
        when(fileRepository.save(any())).thenAnswer(i -> {
            CodeFile c = i.getArgument(0);
            c.setFileId(10L);
            return c;
        });

        CodeFileDto result = fileService.createFile(dto, USER, AUTH);

        assertNotNull(result);
        assertEquals("java", result.getLanguage());
        assertEquals(10L, result.getFileId());
    }

    @Test
    void createFolder_Success() {
        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(1L, "src")).thenReturn(false);
        when(fileRepository.save(any())).thenAnswer(i -> {
            CodeFile c = i.getArgument(0);
            c.setFileId(20L);
            return c;
        });

        CodeFileDto result = fileService.createFolder(1L, "src", "src", USER, AUTH);

        assertNotNull(result);
        assertTrue(result.getIsDirectory());
    }

    @Test
    void getFileById_Success() {
        CodeFile f = CodeFile.builder().fileId(1L).build();
        when(fileRepository.findById(1L)).thenReturn(Optional.of(f));

        CodeFileDto result = fileService.getFileById(1L);
        assertEquals(1L, result.getFileId());
    }

    @Test
    void getFileContent_Success() {
        CodeFile f = CodeFile.builder().fileId(1L).content("test").build();
        when(fileRepository.findById(1L)).thenReturn(Optional.of(f));

        assertEquals("test", fileService.getFileContent(1L));
    }

    @Test
    void getFileTree_Success() {
        CodeFile f1 = CodeFile.builder().fileId(1L).path("src").isDirectory(true).build();
        CodeFile f2 = CodeFile.builder().fileId(2L).path("src/Main.java").isDirectory(false).build();
        CodeFile f3 = CodeFile.builder().fileId(3L).path("pom.xml").isDirectory(false).build();

        when(fileRepository.findByProjectIdAndIsDeletedFalse(1L)).thenReturn(List.of(f2, f3, f1));

        List<CodeFileDto> tree = fileService.getFileTree(1L);
        assertEquals(3, tree.size());
        assertEquals("src", tree.get(0).getPath());
        assertEquals("src/Main.java", tree.get(1).getPath());
        assertEquals("pom.xml", tree.get(2).getPath());
    }

    @Test
    void updateFileContent_Success() {
        CodeFile f = CodeFile.builder().fileId(1L).projectId(1L).isDirectory(false).build();
        when(fileRepository.findById(1L)).thenReturn(Optional.of(f));
        when(fileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CodeFileDto result = fileService.updateFileContent(1L, "new content", USER, AUTH);
        assertEquals("new content", result.getContent());
    }

    @Test
    void renameFile_Success() {
        CodeFile f = CodeFile.builder().fileId(1L).projectId(1L).path("src/old.java").isDirectory(false).build();
        when(fileRepository.findById(1L)).thenReturn(Optional.of(f));
        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(1L, "src/new.java")).thenReturn(false);
        when(fileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CodeFileDto result = fileService.renameFile(1L, "new.java", USER, AUTH);
        assertEquals("src/new.java", result.getPath());
    }

    @Test
    void moveFile_Success() {
        CodeFile f = CodeFile.builder().fileId(1L).projectId(1L).path("old/Main.java").isDirectory(false).build();
        when(fileRepository.findById(1L)).thenReturn(Optional.of(f));
        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(1L, "new/Main.java")).thenReturn(false);
        when(fileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CodeFileDto result = fileService.moveFile(1L, "new/Main.java", USER, AUTH);
        assertEquals("new/Main.java", result.getPath());
    }

    @Test
    void deleteFile_Success() {
        CodeFile f = CodeFile.builder().fileId(1L).projectId(1L).isDirectory(false).build();
        when(fileRepository.findById(1L)).thenReturn(Optional.of(f));

        fileService.deleteFile(1L, USER, AUTH);
        assertTrue(f.getIsDeleted());
        verify(fileRepository).save(f);
    }

    @Test
    void restoreFile_Success() {
        CodeFile f = CodeFile.builder().fileId(1L).projectId(1L).isDirectory(false).build();
        when(fileRepository.findById(1L)).thenReturn(Optional.of(f));

        fileService.restoreFile(1L, USER, AUTH);
        assertFalse(f.getIsDeleted());
        verify(fileRepository).save(f);
    }

    @Test
    void searchInProject_Success() {
        CodeFile f = CodeFile.builder().fileId(1L).projectId(1L).isDirectory(false).build();
        when(fileRepository.searchInProject(1L, "query")).thenReturn(List.of(f));

        List<CodeFileDto> result = fileService.searchInProject(1L, "query");
        assertFalse(result.isEmpty());
        assertEquals(1L, result.get(0).getFileId());
    }

    @Test
    void searchInProject_EmptyQuery_ReturnsAll() {
        CodeFile f = CodeFile.builder().fileId(1L).projectId(1L).isDirectory(false).build();
        when(fileRepository.findByProjectIdAndIsDeletedFalse(1L)).thenReturn(List.of(f));

        List<CodeFileDto> result = fileService.searchInProject(1L, "");
        assertFalse(result.isEmpty());
        assertEquals(1L, result.get(0).getFileId());
    }

    @Test
    void createFile_DuplicatePath_ThrowsException() {
        CodeFileDto dto = new CodeFileDto();
        dto.setProjectId(1L);
        dto.setPath("test.java");
        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(1L, "test.java")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> fileService.createFile(dto, USER, AUTH));
    }

    @Test
    void updateFileContent_OnDirectory_ThrowsException() {
        CodeFile f = CodeFile.builder().fileId(1L).projectId(1L).isDirectory(true).build();
        when(fileRepository.findById(1L)).thenReturn(Optional.of(f));

        assertThrows(RuntimeException.class, () -> fileService.updateFileContent(1L, "content", USER, AUTH));
    }

    @Test
    void renameFile_DuplicatePath_ThrowsException() {
        CodeFile f = CodeFile.builder().fileId(1L).projectId(1L).path("old.java").isDirectory(false).build();
        when(fileRepository.findById(1L)).thenReturn(Optional.of(f));
        when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(1L, "new.java")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> fileService.renameFile(1L, "new.java", USER, AUTH));
    }

    @Test
    void getFileById_NotFound_ThrowsException() {
        when(fileRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> fileService.getFileById(1L));
    }

    @Test
    void detectLanguage_VariousExtensions() {
        // Test different extensions by creating files
        when(fileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String[] extensions = {"js", "ts", "py", "c", "cpp", "go", "rs", "rb", "php", "kt", "swift", "r", "html", "css", "json", "xml", "yaml", "md", "sql", "sh", "dockerfile", "unknown"};
        for (String ext : extensions) {
            CodeFileDto dto = new CodeFileDto();
            dto.setProjectId(1L);
            dto.setPath("test." + ext);
            dto.setName("test." + ext);
            when(fileRepository.existsByProjectIdAndPathAndIsDeletedFalse(1L, "test." + ext)).thenReturn(false);
            fileService.createFile(dto, USER, AUTH);
        }
    }
}
