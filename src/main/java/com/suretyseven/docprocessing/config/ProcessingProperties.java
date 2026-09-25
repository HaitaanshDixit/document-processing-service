package com.suretyseven.docprocessing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.processing")
@Getter
@Setter
public class ProcessingProperties {
    private int maxAttempts;
    private long backoffInitialMs;
    private double backoffMultiplier;
    private long backoffMaxMs;
    private int workerThreads;
    private long lockTtlSeconds;
    private long queuePollTimeoutSeconds;
    private long staleProcessingThresholdSeconds;
    private long recoveryIntervalMs;
}
