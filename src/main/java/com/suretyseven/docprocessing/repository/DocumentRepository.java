package com.suretyseven.docprocessing.repository;

import com.suretyseven.docprocessing.entity.Document;
import com.suretyseven.docprocessing.entity.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, String>, JpaSpecificationExecutor<Document> {

    Optional<Document> findByFileHash(String fileHash);

    List<Document> findByStatusAndUpdatedAtBefore(DocumentStatus status, Instant updatedAtBefore);

    long countByStatus(DocumentStatus status);
}
