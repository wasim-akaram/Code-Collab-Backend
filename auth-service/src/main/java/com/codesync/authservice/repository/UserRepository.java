/*
 * Code reader note: Provides database access methods for User records.
 * Annotations used: @Repository marks the persistence layer, @Query defines the custom
 * username search, and @Param binds the search parameter into that query.
 */
package com.codesync.authservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.codesync.authservice.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by email.
    Optional<User> findByEmail(String email);

    // Find user by username.
    Optional<User> findByUsername(String username);

    // Check whether email exists.
    boolean existsByEmail(String email);

    // Check whether username exists.
    boolean existsByUsername(String username);

    // Search users by username (case-insensitive partial match).
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) AND u.active = true")
    List<User> searchByUsername(@Param("q") String q);

    // Find all users by role (e.g., ADMIN, DEVELOPER).
    List<User> findAllByRole(String role);
}