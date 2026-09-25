package com.suretyseven.docprocessing.support;

import com.suretyseven.docprocessing.service.LockService;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLockService implements LockService {

    private final Set<String> locks = ConcurrentHashMap.newKeySet();

    @Override
    public boolean tryLock(String key, Duration ttl) {
        return locks.add(key);
    }

    @Override
    public void unlock(String key) {
        locks.remove(key);
    }
}
