package com.suretyseven.docprocessing.support;

import com.suretyseven.docprocessing.service.DuplicateDetectionService;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDuplicateDetectionService implements DuplicateDetectionService {

    private final Map<String, String> reservations = new ConcurrentHashMap<>();

    @Override
    public synchronized Optional<String> reserve(String fileHash, String candidateDocumentId) {
        String existing = reservations.putIfAbsent(fileHash, candidateDocumentId);
        return Optional.ofNullable(existing);
    }

    @Override
    public void release(String fileHash) {
        reservations.remove(fileHash);
    }
}
