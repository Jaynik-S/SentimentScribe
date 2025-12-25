package com.sentimentscribe.persistence.postgres.repo;

import com.sentimentscribe.persistence.postgres.entity.DiaryEntryEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryEntryJpaRepository extends JpaRepository<DiaryEntryEntity, UUID> {
    Optional<DiaryEntryEntity> findByUser_IdAndStoragePath(UUID userId, String storagePath);

    List<DiaryEntryEntity> findAllByUser_Id(UUID userId);
}
