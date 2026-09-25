package com.suretyseven.docprocessing.repository;

import com.suretyseven.docprocessing.entity.ExtractedResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExtractedResultRepository extends JpaRepository<ExtractedResult, Long> {

    Optional<ExtractedResult> findByDocumentId(String documentId);
}
