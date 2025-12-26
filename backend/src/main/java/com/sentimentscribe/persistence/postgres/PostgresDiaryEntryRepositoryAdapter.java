package com.sentimentscribe.persistence.postgres;

import com.sentimentscribe.data.DiaryEntryRepository;
import com.sentimentscribe.domain.DiaryEntry;
import com.sentimentscribe.persistence.postgres.entity.DiaryEntryEntity;
import com.sentimentscribe.persistence.postgres.entity.UserEntity;
import com.sentimentscribe.persistence.postgres.repo.DiaryEntryJpaRepository;
import com.sentimentscribe.persistence.postgres.repo.UserJpaRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PostgresDiaryEntryRepositoryAdapter implements DiaryEntryRepository {
    private final DiaryEntryJpaRepository entryRepository;
    private final UserJpaRepository userRepository;
    private final StoragePathGenerator storagePathGenerator;

    public PostgresDiaryEntryRepositoryAdapter(DiaryEntryJpaRepository entryRepository,
                                               UserJpaRepository userRepository,
                                               StoragePathGenerator storagePathGenerator) {
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
        this.storagePathGenerator = storagePathGenerator;
    }

    @Override
    public DiaryEntry getByPath(UUID userId, String entryPath) throws Exception {
        if (entryPath == null || entryPath.isBlank()) {
            return null;
        }
        if (userId == null) {
            throw new Exception("User is required.");
        }
        Optional<DiaryEntryEntity> existing = entryRepository.findByUser_IdAndStoragePath(userId, entryPath);
        if (existing.isEmpty()) {
            return null;
        }
        DiaryEntryEntity entity = existing.get();
        return new DiaryEntry(
                entity.getTitleCiphertext(),
                entity.getTitleIv(),
                entity.getBodyCiphertext(),
                entity.getBodyIv(),
                entity.getAlgo(),
                entity.getVersion(),
                entity.getStoragePath(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    @Override
    public boolean deleteByPath(UUID userId, String entryPath) {
        if (entryPath == null || entryPath.isBlank()) {
            return false;
        }
        if (userId == null) {
            return false;
        }
        Optional<DiaryEntryEntity> existing = entryRepository.findByUser_IdAndStoragePath(userId, entryPath);
        if (existing.isEmpty()) {
            return false;
        }
        entryRepository.delete(existing.get());
        return true;
    }

    @Override
    public boolean save(UUID userId, DiaryEntry entry) throws Exception {
        if (entry == null) {
            throw new Exception("Entry cannot be null.");
        }
        if (userId == null) {
            throw new Exception("User is required.");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found."));

        LocalDateTime createdAt = entry.getCreatedAt() != null ? entry.getCreatedAt() : LocalDateTime.now();
        LocalDateTime updatedAt = entry.getUpdatedAt() != null ? entry.getUpdatedAt() : LocalDateTime.now();

        String storagePath = entry.getStoragePath();
        DiaryEntryEntity entity;
        if (storagePath != null && !storagePath.isBlank()) {
            entity = entryRepository.findByUser_IdAndStoragePath(userId, storagePath)
                    .orElseGet(DiaryEntryEntity::new);
        }
        else {
            storagePath = storagePathGenerator.generate();
            entry.setStoragePath(storagePath);
            entity = new DiaryEntryEntity();
        }

        entity.setUser(user);
        entity.setStoragePath(storagePath);
        entity.setTitleCiphertext(entry.getTitleCiphertext());
        entity.setTitleIv(entry.getTitleIv());
        entity.setBodyCiphertext(entry.getBodyCiphertext());
        entity.setBodyIv(entry.getBodyIv());
        entity.setAlgo(entry.getAlgo());
        entity.setVersion(entry.getVersion());
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);
        entryRepository.save(entity);
        return true;
    }

    @Override
    public List<Map<String, Object>> getAll(UUID userId) throws Exception {
        if (userId == null) {
            throw new Exception("User is required.");
        }
        return entryRepository.findAllByUser_Id(userId).stream()
                .map(this::toSummaryMap)
                .toList();
    }

    private Map<String, Object> toSummaryMap(DiaryEntryEntity entity) {
        Map<String, Object> result = new HashMap<>();
        result.put("titleCiphertext", entity.getTitleCiphertext());
        result.put("titleIv", entity.getTitleIv());
        result.put("algo", entity.getAlgo());
        result.put("version", entity.getVersion());
        result.put("createdDate", entity.getCreatedAt());
        result.put("updatedDate", entity.getUpdatedAt());
        result.put("storagePath", entity.getStoragePath());
        return result;
    }
}
