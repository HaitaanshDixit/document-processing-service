package com.suretyseven.docprocessing.service.impl;

import com.suretyseven.docprocessing.config.ProcessingProperties;
import com.suretyseven.docprocessing.dto.ExtractedResultDto;
import com.suretyseven.docprocessing.entity.Document;
import com.suretyseven.docprocessing.entity.DocumentHistoryEntry;
import com.suretyseven.docprocessing.entity.DocumentStatus;
import com.suretyseven.docprocessing.entity.ExtractedResult;
import com.suretyseven.docprocessing.entity.FailureReasons;
import com.suretyseven.docprocessing.entity.ProcessorOutcomeType;
import com.suretyseven.docprocessing.exception.NonRetryableProcessingException;
import com.suretyseven.docprocessing.exception.RetryableProcessingException;
import com.suretyseven.docprocessing.repository.DocumentHistoryRepository;
import com.suretyseven.docprocessing.repository.DocumentRepository;
import com.suretyseven.docprocessing.repository.ExtractedResultRepository;
import com.suretyseven.docprocessing.service.DocumentProcessor;
import com.suretyseven.docprocessing.service.LockService;
import com.suretyseven.docprocessing.service.ProcessingService;
import com.suretyseven.docprocessing.service.ValidationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessingServiceImpl implements ProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ProcessingServiceImpl.class);
    private static final String LOCK_PREFIX = "lock:document:";

    private final DocumentRepository documentRepository;
    private final ExtractedResultRepository extractedResultRepository;
    private final DocumentHistoryRepository historyRepository;
    private final DocumentProcessor documentProcessor;
    private final ValidationService validationService;
    private final LockService lockService;
    private final RetryTemplate processingRetryTemplate;
    private final ProcessingProperties processingProperties;

    @Override
    public void process(String documentId) {
        MDC.put("documentId", documentId);
        String lockKey = LOCK_PREFIX + documentId;

        if (!lockService.tryLock(lockKey, Duration.ofSeconds(processingProperties.getLockTtlSeconds()))) {
            log.info("Document is already being processed elsewhere, skipping");
            MDC.remove("documentId");
            return;
        }

        try {
            Document document = documentRepository.findById(documentId).orElse(null);
            if (document == null) {
                log.warn("Document referenced by queue no longer exists");
                return;
            }
            if (document.getStatus() == DocumentStatus.PROCESSED) {
                log.info("Document already processed, skipping");
                return;
            }

            transitionTo(document, DocumentStatus.PROCESSING, null, null);

            processingRetryTemplate.execute(
                    (RetryCallback<Void, RuntimeException>) context -> attempt(document, context),
                    context -> {
                        recoverAfterExhaustion(document, context);
                        return null;
                    });
        } finally {
            lockService.unlock(lockKey);
            MDC.remove("documentId");
        }
    }

    private Void attempt(Document document, RetryContext context) {
        int attemptNumber = context.getRetryCount() + 1;
        log.info("Processing attempt {} of {}", attemptNumber, processingProperties.getMaxAttempts());

        if (context.getRetryCount() > 0) {
            document.setRetryCount(context.getRetryCount());
            documentRepository.save(document);
            transitionTo(document, DocumentStatus.PROCESSING, null, null);
        }

        DocumentProcessor.ProcessorOutcome outcome = documentProcessor.process(document);

        if (outcome.getType() == ProcessorOutcomeType.SUCCESS) {
            handleSuccess(document, outcome.getData());
            return null;
        }

        String reason = mapReason(outcome.getType());
        log.warn("Processing attempt {} failed, reason={}", attemptNumber, reason);
        transitionTo(document, DocumentStatus.FAILED, reason, null);
        throw new RetryableProcessingException(reason);
    }

    private void handleSuccess(Document document, ExtractedResultDto data) {
        List<String> errors = validationService.validate(data);
        if (!errors.isEmpty()) {
            log.warn("Extracted data failed validation, errors={}", errors);
            saveExtractedResult(document, data, errors);
            transitionTo(document, DocumentStatus.FAILED, FailureReasons.VALIDATION_FAILED, String.join("|", errors));
            throw new NonRetryableProcessingException(String.join("|", errors));
        }

        saveExtractedResult(document, data, List.of());
        document.setFailureReason(null);
        transitionTo(document, DocumentStatus.PROCESSED, null, null);
        log.info("Document processed successfully");
    }

    private void recoverAfterExhaustion(Document document, RetryContext context) {
        Throwable last = context.getLastThrowable();
        if (last instanceof NonRetryableProcessingException) {
            return;
        }
        String lastReason = last != null ? last.getMessage() : "unknown";
        log.error("Exhausted retries, giving up. lastReason={}", lastReason);
        transitionTo(document, DocumentStatus.FAILED, FailureReasons.MAX_RETRIES_EXCEEDED,
                "Last failure: " + lastReason);
    }

    private void saveExtractedResult(Document document, ExtractedResultDto data, List<String> errors) {
        ExtractedResult result = extractedResultRepository.findByDocumentId(document.getId())
                .orElseGet(ExtractedResult::new);
        result.setDocumentId(document.getId());
        if (data != null) {
            result.setCompanyName(data.getCompanyName());
            result.setRegistrationNumber(data.getRegistrationNumber());
            result.setAddress(data.getAddress());
            result.setAnnualRevenue(data.getAnnualRevenue());
            result.setDocumentDate(data.getDocumentDate());
        }
        result.setValidationErrors(errors.isEmpty() ? null : String.join("|", errors));
        extractedResultRepository.save(result);
    }

    private void transitionTo(Document document, DocumentStatus status, String reason, String detail) {
        document.setStatus(status);
        document.setFailureReason(reason);
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);

        historyRepository.save(DocumentHistoryEntry.builder()
                .documentId(document.getId())
                .status(status)
                .reason(reason)
                .detail(detail)
                .timestamp(Instant.now())
                .build());
    }

    private String mapReason(ProcessorOutcomeType type) {
        return switch (type) {
            case TIMEOUT -> FailureReasons.PROCESSOR_TIMEOUT;
            case ERROR -> FailureReasons.PROCESSOR_ERROR;
            case INVALID_RESULT -> FailureReasons.INVALID_EXTRACTION_RESULT;
            case SUCCESS -> throw new IllegalStateException("SUCCESS is not a failure reason");
        };
    }
}
