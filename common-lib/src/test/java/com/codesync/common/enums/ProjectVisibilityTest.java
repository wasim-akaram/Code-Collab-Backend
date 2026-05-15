package com.codesync.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectVisibilityTest {

    @Test
    void enum_shouldHavePublicValue() {
        ProjectVisibility visibility = ProjectVisibility.PUBLIC;
        assertEquals("PUBLIC", visibility.name());
    }

    @Test
    void enum_shouldHavePrivateValue() {
        ProjectVisibility visibility = ProjectVisibility.PRIVATE;
        assertEquals("PRIVATE", visibility.name());
    }

    @Test
    void enum_shouldHaveExactlyTwoValues() {
        ProjectVisibility[] values = ProjectVisibility.values();
        assertEquals(2, values.length);
    }

    @Test
    void valueOf_shouldReturnCorrectEnum() {
        assertEquals(ProjectVisibility.PUBLIC, ProjectVisibility.valueOf("PUBLIC"));
        assertEquals(ProjectVisibility.PRIVATE, ProjectVisibility.valueOf("PRIVATE"));
    }

    @Test
    void valueOf_shouldThrowForInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> ProjectVisibility.valueOf("INTERNAL"));
    }

    @Test
    void ordinal_shouldBeCorrect() {
        assertEquals(0, ProjectVisibility.PUBLIC.ordinal());
        assertEquals(1, ProjectVisibility.PRIVATE.ordinal());
    }
}
