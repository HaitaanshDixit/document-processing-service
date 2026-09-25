package com.suretyseven.docprocessing.service;

import java.util.Optional;

public interface DuplicateDetectionService {

    Optional<String> reserve(String fileHash, String candidateDocumentId);

    void release(String fileHash);
}
