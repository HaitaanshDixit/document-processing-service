package com.suretyseven.docprocessing.repository;

import com.suretyseven.docprocessing.entity.DocumentHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentHistoryRepository extends JpaRepository<DocumentHistoryEntry, Long> {

    List<DocumentHistoryEntry> findByDocumentIdOrderByTimestampAsc(String documentId);
}
