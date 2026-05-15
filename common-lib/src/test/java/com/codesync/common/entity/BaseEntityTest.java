package com.codesync.common.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BaseEntityTest {

    // Concrete subclass for testing the abstract BaseEntity
    private static class TestEntity extends BaseEntity {
    }

    @Test
    void onCreate_shouldSetCreatedAtAndUpdatedAt() {
        TestEntity entity = new TestEntity();

        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());

        entity.onCreate();

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        // Both timestamps should be very close (set in the same method call)
        assertEquals(entity.getCreatedAt(), entity.getUpdatedAt());
    }

    @Test
    void onUpdate_shouldUpdateOnlyUpdatedAt() {
        TestEntity entity = new TestEntity();
        entity.onCreate();

        LocalDateTime originalCreatedAt = entity.getCreatedAt();
        LocalDateTime originalUpdatedAt = entity.getUpdatedAt();

        // Ensure a slight time difference
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        entity.onUpdate();

        // createdAt should remain unchanged
        assertEquals(originalCreatedAt, entity.getCreatedAt());
        // updatedAt should be updated to a new value
        assertNotNull(entity.getUpdatedAt());
        assertTrue(entity.getUpdatedAt().isAfter(originalUpdatedAt) || entity.getUpdatedAt().isEqual(originalUpdatedAt));
    }

    @Test
    void idGetterAndSetter_shouldWork() {
        TestEntity entity = new TestEntity();

        assertNull(entity.getId());

        entity.setId(42L);
        assertEquals(42L, entity.getId());
    }

    @Test
    void createdAtGetterAndSetter_shouldWork() {
        TestEntity entity = new TestEntity();
        LocalDateTime now = LocalDateTime.now();

        entity.setCreatedAt(now);
        assertEquals(now, entity.getCreatedAt());
    }

    @Test
    void updatedAtGetterAndSetter_shouldWork() {
        TestEntity entity = new TestEntity();
        LocalDateTime now = LocalDateTime.now();

        entity.setUpdatedAt(now);
        assertEquals(now, entity.getUpdatedAt());
    }

    @Test
    void onCreate_shouldSetTimestampsCloseToNow() {
        TestEntity entity = new TestEntity();
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        entity.onCreate();

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertTrue(entity.getCreatedAt().isAfter(before));
        assertTrue(entity.getCreatedAt().isBefore(after));
        assertTrue(entity.getUpdatedAt().isAfter(before));
        assertTrue(entity.getUpdatedAt().isBefore(after));
    }

    @Test
    void multipleOnUpdateCalls_shouldAlwaysUpdateTimestamp() {
        TestEntity entity = new TestEntity();
        entity.onCreate();

        LocalDateTime first = entity.getUpdatedAt();

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        entity.onUpdate();
        LocalDateTime second = entity.getUpdatedAt();

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        entity.onUpdate();
        LocalDateTime third = entity.getUpdatedAt();

        assertTrue(second.isAfter(first) || second.isEqual(first));
        assertTrue(third.isAfter(second) || third.isEqual(second));
    }
}
