package com.codesync.common.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResponseTest {

    @Test
    void builder_shouldCreatePageResponseWithAllFields() {
        List<String> content = Arrays.asList("item1", "item2", "item3");

        PageResponse<String> response = PageResponse.<String>builder()
                .content(content)
                .page(0)
                .size(10)
                .totalElements(3L)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();

        assertEquals(content, response.getContent());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(3L, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    void noArgsConstructor_shouldCreateEmptyPageResponse() {
        PageResponse<String> response = new PageResponse<>();

        assertNull(response.getContent());
        assertEquals(0, response.getPage());
        assertEquals(0, response.getSize());
        assertEquals(0L, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertFalse(response.isFirst());
        assertFalse(response.isLast());
    }

    @Test
    void allArgsConstructor_shouldSetAllFields() {
        List<Integer> content = List.of(1, 2, 3);

        PageResponse<Integer> response = new PageResponse<>(content, 2, 5, 25L, 5, false, false);

        assertEquals(content, response.getContent());
        assertEquals(2, response.getPage());
        assertEquals(5, response.getSize());
        assertEquals(25L, response.getTotalElements());
        assertEquals(5, response.getTotalPages());
        assertFalse(response.isFirst());
        assertFalse(response.isLast());
    }

    @Test
    void setters_shouldUpdateFields() {
        PageResponse<String> response = new PageResponse<>();
        List<String> content = List.of("a", "b");

        response.setContent(content);
        response.setPage(1);
        response.setSize(20);
        response.setTotalElements(100L);
        response.setTotalPages(5);
        response.setFirst(true);
        response.setLast(false);

        assertEquals(content, response.getContent());
        assertEquals(1, response.getPage());
        assertEquals(20, response.getSize());
        assertEquals(100L, response.getTotalElements());
        assertEquals(5, response.getTotalPages());
        assertTrue(response.isFirst());
        assertFalse(response.isLast());
    }

    @Test
    void builder_withEmptyContent_shouldWork() {
        PageResponse<Object> response = PageResponse.builder()
                .content(Collections.emptyList())
                .page(0)
                .size(10)
                .totalElements(0L)
                .totalPages(0)
                .isFirst(true)
                .isLast(true)
                .build();

        assertNotNull(response.getContent());
        assertTrue(response.getContent().isEmpty());
        assertEquals(0L, response.getTotalElements());
    }

    @Test
    void equals_shouldWorkCorrectly() {
        PageResponse<String> response1 = PageResponse.<String>builder()
                .content(List.of("a"))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();

        PageResponse<String> response2 = PageResponse.<String>builder()
                .content(List.of("a"))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();

        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    void toString_shouldNotBeNull() {
        PageResponse<String> response = PageResponse.<String>builder()
                .content(List.of("test"))
                .build();

        assertNotNull(response.toString());
        assertTrue(response.toString().contains("test"));
    }

    @Test
    void genericType_shouldSupportDifferentTypes() {
        // With Integer
        PageResponse<Integer> intResponse = PageResponse.<Integer>builder()
                .content(List.of(1, 2, 3))
                .totalElements(3L)
                .build();
        assertEquals(3, intResponse.getContent().size());

        // With custom type
        PageResponse<PageResponse<String>> nestedResponse = PageResponse.<PageResponse<String>>builder()
                .content(List.of(PageResponse.<String>builder().build()))
                .totalElements(1L)
                .build();
        assertEquals(1, nestedResponse.getContent().size());
    }

    @Test
    void middlePage_shouldNotBeFirstOrLast() {
        PageResponse<String> response = PageResponse.<String>builder()
                .content(List.of("item"))
                .page(1)
                .size(10)
                .totalElements(30L)
                .totalPages(3)
                .isFirst(false)
                .isLast(false)
                .build();

        assertFalse(response.isFirst());
        assertFalse(response.isLast());
        assertEquals(1, response.getPage());
    }
}
