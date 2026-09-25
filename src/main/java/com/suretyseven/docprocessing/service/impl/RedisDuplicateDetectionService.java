package com.suretyseven.docprocessing.service.impl;

import com.suretyseven.docprocessing.service.DuplicateDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisDuplicateDetectionService implements DuplicateDetectionService {

    private static final String KEY_PREFIX = "dup:filehash:";
    private static final Duration TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;

    @Override
    public Optional<String> reserve(String fileHash, String candidateDocumentId) {
        String key = KEY_PREFIX + fileHash;
        Boolean reserved = redisTemplate.opsForValue().setIfAbsent(key, candidateDocumentId, TTL);
        if (Boolean.TRUE.equals(reserved)) {
            return Optional.empty();
        }
        String existing = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(existing);
    }

    @Override
    public void release(String fileHash) {
        redisTemplate.delete(KEY_PREFIX + fileHash);
    }
}
