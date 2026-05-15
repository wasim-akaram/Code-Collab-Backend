/*
 * Code reader note: Represents an application user persisted in the auth database.
 * Annotations used: @Entity and @Table map the class to the users table,
 * Lombok annotations (@Data, @NoArgsConstructor, @AllArgsConstructor, @Builder)
 * generate boilerplate, and JPA annotations (@Id, @GeneratedValue, @Column)
 * define the primary key and column constraints.
 */
package com.codesync.authservice.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    // Primary key.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique public username.
    @Column(unique = true)
    private String username;

    // Unique email used for login.
    @Column(unique = true)
    private String email;

    // Full display name of the user.
    private String fullName;

    // Hashed password.
    private String password;

    // Login provider (LOCAL / GOOGLE / GITHUB).
    private String provider; // LOCAL / GOOGLE / GITHUB

    // User role (DEVELOPER / ADMIN).
    private String role;

    // Account activation flag.
    private boolean active;

    // Account creation timestamp.
    private Instant createdAt;

    // Short user biography (profile page).
    @Column(length = 500)
    private String bio;

    // URL to the user's profile picture.
    @Column(length = 1000)
    private String avatarUrl;

    // Subscription plan: FREE or PRO (defaults to FREE for all existing users).
    @Column(length = 10)
    @Builder.Default
    private String plan = "FREE";

    // When the Pro plan expires (null = never / not applicable).
    private Instant planExpiresAt;
}