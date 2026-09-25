package com.suretyseven.docprocessing.support;

import com.suretyseven.docprocessing.service.QueueService;

import java.time.Duration;
import java.util.LinkedList;
import java.util.Optional;

public class InMemoryQueueService implements QueueService {

    private final LinkedList<String> queue = new LinkedList<>();

    @Override
    public synchronized void enqueue(String documentId) {
        queue.addFirst(documentId);
    }

    @Override
    public synchronized Optional<String> dequeue(Duration timeout) {
        if (queue.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(queue.removeLast());
    }
}
