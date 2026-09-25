package com.suretyseven.docprocessing.service;

import java.time.Duration;
import java.util.Optional;

public interface QueueService {

    void enqueue(String documentId);

    Optional<String> dequeue(Duration timeout);
}
