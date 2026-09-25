package com.suretyseven.docprocessing.service.impl;

import com.suretyseven.docprocessing.service.LockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RedisLockService implements LockService {

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, String> ownershipTokens = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        if (Boolean.TRUE.equals(acquired)) {
            ownershipTokens.put(key, token);
            return true;
        }
        return false;
    }

    @Override
    public void unlock(String key) {
        String token = ownershipTokens.remove(key);
        if (token == null) {
            return;
        }
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        redisTemplate.execute(script, Collections.singletonList(key), token);
    }
}
