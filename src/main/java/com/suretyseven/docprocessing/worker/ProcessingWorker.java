package com.suretyseven.docprocessing.worker;

import com.suretyseven.docprocessing.config.ProcessingProperties;
import com.suretyseven.docprocessing.service.ProcessingService;
import com.suretyseven.docprocessing.service.QueueService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ProcessingWorker {

    private static final Logger log = LoggerFactory.getLogger(ProcessingWorker.class);

    private final QueueService queueService;
    private final ProcessingService processingService;
    private final ProcessingProperties processingProperties;

    private ExecutorService executorService;
    private volatile boolean running = true;

    @PostConstruct
    public void start() {
        int threadCount = Math.max(processingProperties.getWorkerThreads(), 1);
        executorService = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(this::runLoop);
        }
        log.info("Started {} document processing worker thread(s)", threadCount);
    }

    private void runLoop() {
        Duration pollTimeout = Duration.ofSeconds(processingProperties.getQueuePollTimeoutSeconds());
        while (running) {
            try {
                queueService.dequeue(pollTimeout).ifPresent(processingService::process);
            } catch (Exception e) {
                if (running) {
                    log.error("Unexpected error in processing worker loop, retrying shortly", e);
                    sleepQuietly(Duration.ofSeconds(2));
                }
            }
        }
    }

    private void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
