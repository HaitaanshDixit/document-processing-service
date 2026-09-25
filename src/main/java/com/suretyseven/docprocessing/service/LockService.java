package com.suretyseven.docprocessing.service;

import java.time.Duration;

public interface LockService {

    boolean tryLock(String key, Duration ttl);

    void unlock(String key);
}
