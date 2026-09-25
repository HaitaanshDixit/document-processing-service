package com.suretyseven.docprocessing.worker;

import com.suretyseven.docprocessing.config.ProcessingProperties;
import com.suretyseven.docprocessing.entity.Document;
import com.suretyseven.docprocessing.entity.DocumentHistoryEntry;
import com.suretyseven.docprocessing.entity.DocumentStatus;
import com.suretyseven.docprocessing.entity.FailureReasons;
import com.suretyseven.docprocessing.repository.DocumentHistoryRepository;
import com.suretyseven.docprocessing.repository.DocumentRepository;
import com.suretyseven.docprocessing.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StaleDocumentRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(StaleDocumentRecoveryScheduler.class);

    private final DocumentRepository documentRepository;
    private final DocumentHistoryRepository historyRepository;
    private final QueueService queueService;
    private final ProcessingProperties processingProperties;

    @Scheduled(fixedDelayString = "${app.processing.recovery-interval-ms}")
    public void recoverStaleDocuments() {
        Instant threshold = Instant.now().minusSeconds(processingProperties.getStaleProcessingThresholdSeconds());
        List<Document> stale = documentRepository.findByStatusAndUpdatedAtBefore(DocumentStatus.PROCESSING, threshold);

        for (Document document : stale) {
            log.warn("Recovering stale document stuck in PROCESSING, documentId={}", document.getId());
            historyRepository.save(DocumentHistoryEntry.builder()
                    .documentId(document.getId())
                    .status(DocumentStatus.PROCESSING)
                    .reason(FailureReasons.REQUEUED_AFTER_CRASH_RECOVERY)
                    .detail("No update for over " + processingProperties.getStaleProcessingThresholdSeconds() + "s, requeuing")
                    .timestamp(Instant.now())
                    .build());
            queueService.enqueue(document.getId());
        }
    }
}
