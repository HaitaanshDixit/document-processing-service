package com.suretyseven.docprocessing.service.impl;

import com.suretyseven.docprocessing.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisQueueService implements QueueService {

    private static final String QUEUE_KEY = "queue:document-processing";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void enqueue(String documentId) {
        redisTemplate.opsForList().leftPush(QUEUE_KEY, documentId);
    }

    @Override
    public Optional<String> dequeue(Duration timeout) {
        return Optional.ofNullable(redisTemplate.opsForList().rightPop(QUEUE_KEY, timeout));
    }
}
