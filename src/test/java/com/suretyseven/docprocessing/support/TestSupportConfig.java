package com.suretyseven.docprocessing.support;

import com.suretyseven.docprocessing.service.DuplicateDetectionService;
import com.suretyseven.docprocessing.service.LockService;
import com.suretyseven.docprocessing.service.QueueService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestSupportConfig {

    @Bean
    @Primary
    public QueueService queueService() {
        return new InMemoryQueueService();
    }

    @Bean
    @Primary
    public DuplicateDetectionService duplicateDetectionService() {
        return new InMemoryDuplicateDetectionService();
    }

    @Bean
    @Primary
    public LockService lockService() {
        return new InMemoryLockService();
    }
}
