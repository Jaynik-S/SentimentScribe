package com.sentimentscribe.persistence.postgres.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "diary_entries",
        uniqueConstraints = @UniqueConstraint(columnNames = "storage_path")
)
public class DiaryEntryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "storage_path", nullable = false, unique = true)
    private String storagePath;

    @Column(name = "title_ciphertext", nullable = false)
    private String titleCiphertext;

    @Column(name = "title_iv", nullable = false)
    private String titleIv;

    @Column(name = "body_ciphertext", nullable = false)
    private String bodyCiphertext;

    @Column(name = "body_iv", nullable = false)
    private String bodyIv;

    @Column(nullable = false)
    private String algo;

    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public DiaryEntryEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getTitleCiphertext() {
        return titleCiphertext;
    }

    public void setTitleCiphertext(String titleCiphertext) {
        this.titleCiphertext = titleCiphertext;
    }

    public String getTitleIv() {
        return titleIv;
    }

    public void setTitleIv(String titleIv) {
        this.titleIv = titleIv;
    }

    public String getBodyCiphertext() {
        return bodyCiphertext;
    }

    public void setBodyCiphertext(String bodyCiphertext) {
        this.bodyCiphertext = bodyCiphertext;
    }

    public String getBodyIv() {
        return bodyIv;
    }

    public void setBodyIv(String bodyIv) {
        this.bodyIv = bodyIv;
    }

    public String getAlgo() {
        return algo;
    }

    public void setAlgo(String algo) {
        this.algo = algo;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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
