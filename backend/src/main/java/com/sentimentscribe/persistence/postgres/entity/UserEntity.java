package com.sentimentscribe.persistence.postgres.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "e2ee_kdf", nullable = false)
    private String e2eeKdf;

    @Column(name = "e2ee_salt", nullable = false)
    private byte[] e2eeSalt;

    @Column(name = "e2ee_iterations", nullable = false)
    private int e2eeIterations;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UserEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getE2eeKdf() {
        return e2eeKdf;
    }

    public void setE2eeKdf(String e2eeKdf) {
        this.e2eeKdf = e2eeKdf;
    }

    public byte[] getE2eeSalt() {
        return e2eeSalt;
    }

    public void setE2eeSalt(byte[] e2eeSalt) {
        this.e2eeSalt = e2eeSalt;
    }

    public int getE2eeIterations() {
        return e2eeIterations;
    }

    public void setE2eeIterations(int e2eeIterations) {
        this.e2eeIterations = e2eeIterations;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
