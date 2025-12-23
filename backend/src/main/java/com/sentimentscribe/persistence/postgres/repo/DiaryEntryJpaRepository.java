package com.sentimentscribe.persistence.postgres.repo;

import com.sentimentscribe.persistence.postgres.entity.DiaryEntryEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryEntryJpaRepository extends JpaRepository<DiaryEntryEntity, UUID> {
    Optional<DiaryEntryEntity> findByStoragePath(String storagePath);
}
