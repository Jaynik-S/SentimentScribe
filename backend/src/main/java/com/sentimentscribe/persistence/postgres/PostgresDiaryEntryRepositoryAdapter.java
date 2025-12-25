package com.sentimentscribe.persistence.postgres;

import com.sentimentscribe.data.DiaryEntryRepository;
import com.sentimentscribe.domain.DiaryEntry;
import com.sentimentscribe.persistence.postgres.entity.DiaryEntryEntity;
import com.sentimentscribe.persistence.postgres.entity.UserEntity;
import com.sentimentscribe.persistence.postgres.repo.DiaryEntryJpaRepository;
import com.sentimentscribe.persistence.postgres.repo.UserJpaRepository;
import com.sentimentscribe.usecase.save_entry.SaveEntryKeywordExtractor;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PostgresDiaryEntryRepositoryAdapter implements DiaryEntryRepository {
    private final DiaryEntryJpaRepository entryRepository;
    private final UserJpaRepository userRepository;
    private final SaveEntryKeywordExtractor keywordExtractor;
    private final StoragePathGenerator storagePathGenerator;

    public PostgresDiaryEntryRepositoryAdapter(DiaryEntryJpaRepository entryRepository,
                                               UserJpaRepository userRepository,
                                               SaveEntryKeywordExtractor keywordExtractor,
                                               StoragePathGenerator storagePathGenerator) {
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
        this.keywordExtractor = keywordExtractor;
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
        List<String> keywords = extractKeywords(entity.getTitle(), entity.getText());
        return new DiaryEntry(
                entity.getTitle(),
                entity.getText(),
                keywords,
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
        entity.setTitle(entry.getTitle());
        entity.setText(entry.getText());
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
        result.put("title", entity.getTitle());
        result.put("text", entity.getText());
        result.put("keywords", extractKeywords(entity.getTitle(), entity.getText()));
        result.put("createdDate", entity.getCreatedAt());
        result.put("updatedDate", entity.getUpdatedAt());
        result.put("storagePath", entity.getStoragePath());
        return result;
    }

    private List<String> extractKeywords(String title, String text) {
        if (keywordExtractor == null) {
            return List.of();
        }
        StringBuilder builder = new StringBuilder();
        if (title != null && !title.isBlank()) {
            builder.append(title);
        }
        if (text != null && !text.isBlank()) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append(text);
        }
        if (builder.length() == 0) {
            return List.of();
        }
        try {
            return keywordExtractor.extractKeywords(builder.toString());
        }
        catch (Exception ignored) {
            return List.of();
        }
    }
}
