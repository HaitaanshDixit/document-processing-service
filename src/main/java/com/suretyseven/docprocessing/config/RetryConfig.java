package com.suretyseven.docprocessing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate processingRetryTemplate(
            @Value("${app.processing.max-attempts}") int maxAttempts,
            @Value("${app.processing.backoff-initial-ms}") long backoffInitialMs,
            @Value("${app.processing.backoff-multiplier}") double backoffMultiplier,
            @Value("${app.processing.backoff-max-ms}") long backoffMaxMs) {
        return RetryTemplateFactory.create(maxAttempts, backoffInitialMs, backoffMultiplier, backoffMaxMs);
    }
}
