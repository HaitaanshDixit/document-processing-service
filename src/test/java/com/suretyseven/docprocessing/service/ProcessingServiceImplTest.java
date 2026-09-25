package com.suretyseven.docprocessing.service;

import com.suretyseven.docprocessing.config.ProcessingProperties;
import com.suretyseven.docprocessing.config.RetryTemplateFactory;
import com.suretyseven.docprocessing.dto.ExtractedResultDto;
import com.suretyseven.docprocessing.entity.Document;
import com.suretyseven.docprocessing.entity.DocumentStatus;
import com.suretyseven.docprocessing.entity.ExtractedResult;
import com.suretyseven.docprocessing.entity.FailureReasons;
import com.suretyseven.docprocessing.entity.ProcessorOutcomeType;
import com.suretyseven.docprocessing.repository.DocumentHistoryRepository;
import com.suretyseven.docprocessing.repository.DocumentRepository;
import com.suretyseven.docprocessing.repository.ExtractedResultRepository;
import com.suretyseven.docprocessing.service.impl.ProcessingServiceImpl;
import com.suretyseven.docprocessing.support.InMemoryLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessingServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ExtractedResultRepository extractedResultRepository;
    @Mock
    private DocumentHistoryRepository historyRepository;
    @Mock
    private DocumentProcessor documentProcessor;
    @Mock
    private ValidationService validationService;

    private ProcessingServiceImpl processingService;

    private static final String DOC_ID = "DOC-TEST0001";

    @BeforeEach
    void setUp() {
        RetryTemplate retryTemplate = RetryTemplateFactory.create(3, 1, 1.0, 1);

        ProcessingProperties properties = new ProcessingProperties();
        properties.setMaxAttempts(3);
        properties.setLockTtlSeconds(30);

        processingService = new ProcessingServiceImpl(
                documentRepository,
                extractedResultRepository,
                historyRepository,
                documentProcessor,
                validationService,
                new InMemoryLockService(),
                retryTemplate,
                properties
        );

        when(extractedResultRepository.findByDocumentId(DOC_ID)).thenReturn(Optional.empty());
    }

    private Document uploadedDocument() {
        return Document.builder()
                .id(DOC_ID)
                .originalFilename("file.pdf")
                .storedPath("/tmp/file.pdf")
                .documentType("FINANCIAL_STATEMENT")
                .fileHash("hash")
                .status(DocumentStatus.UPLOADED)
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private ExtractedResultDto validData() {
        return ExtractedResultDto.builder()
                .companyName("ABC Construction Pvt Ltd")
                .registrationNumber("U12345DL2020PTC123456")
                .address("New Delhi")
                .annualRevenue(BigDecimal.valueOf(12_500_000))
                .documentDate(LocalDate.of(2026, 8, 15))
                .build();
    }

    @Test
    void successfulProcessing_marksDocumentProcessed() {
        Document document = uploadedDocument();
        when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(document));
        when(documentProcessor.process(any()))
                .thenReturn(new DocumentProcessor.ProcessorOutcome(ProcessorOutcomeType.SUCCESS, validData()));
        when(validationService.validate(any())).thenReturn(List.of());

        processingService.process(DOC_ID);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.PROCESSED);
        verify(documentProcessor, times(1)).process(any());
        verify(extractedResultRepository, times(1)).save(any(ExtractedResult.class));
    }

    @Test
    void processorFailure_afterAllRetriesExhausted_marksFailed() {
        Document document = uploadedDocument();
        when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(document));
        when(documentProcessor.process(any()))
                .thenReturn(new DocumentProcessor.ProcessorOutcome(ProcessorOutcomeType.TIMEOUT, null));

        processingService.process(DOC_ID);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(document.getFailureReason()).isEqualTo(FailureReasons.MAX_RETRIES_EXCEEDED);
        verify(documentProcessor, times(3)).process(any());
    }

    @Test
    void failureFollowedBySuccessfulRetry_marksProcessed() {
        Document document = uploadedDocument();
        when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(document));
        when(documentProcessor.process(any()))
                .thenReturn(new DocumentProcessor.ProcessorOutcome(ProcessorOutcomeType.TIMEOUT, null))
                .thenReturn(new DocumentProcessor.ProcessorOutcome(ProcessorOutcomeType.SUCCESS, validData()));
        when(validationService.validate(any())).thenReturn(List.of());

        processingService.process(DOC_ID);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.PROCESSED);
        verify(documentProcessor, times(2)).process(any());

        ArgumentCaptor<com.suretyseven.docprocessing.entity.DocumentHistoryEntry> historyCaptor =
                ArgumentCaptor.forClass(com.suretyseven.docprocessing.entity.DocumentHistoryEntry.class);
        verify(historyRepository, times(4)).save(historyCaptor.capture());

        List<DocumentStatus> recordedStatuses = historyCaptor.getAllValues().stream()
                .map(com.suretyseven.docprocessing.entity.DocumentHistoryEntry::getStatus)
                .toList();
        assertThat(recordedStatuses).containsExactly(
                DocumentStatus.PROCESSING,
                DocumentStatus.FAILED,
                DocumentStatus.PROCESSING,
                DocumentStatus.PROCESSED
        );
    }

    @Test
    void invalidExtractedData_marksFailedWithoutRetrying() {
        Document document = uploadedDocument();
        when(documentRepository.findById(DOC_ID)).thenReturn(Optional.of(document));
        ExtractedResultDto invalidData = ExtractedResultDto.builder()
                .companyName(null)
                .registrationNumber("U12345DL2020PTC123456")
                .annualRevenue(BigDecimal.valueOf(-5))
                .documentDate(LocalDate.of(2026, 8, 15))
                .build();
        when(documentProcessor.process(any()))
                .thenReturn(new DocumentProcessor.ProcessorOutcome(ProcessorOutcomeType.SUCCESS, invalidData));
        when(validationService.validate(any())).thenReturn(List.of("companyName is required", "annualRevenue must be greater than or equal to 0"));

        processingService.process(DOC_ID);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(document.getFailureReason()).isEqualTo(FailureReasons.VALIDATION_FAILED);
        verify(documentProcessor, times(1)).process(any());
    }
}
