package com.suretyseven.docprocessing.service.impl;

import com.suretyseven.docprocessing.dto.DocumentDetailResponse;
import com.suretyseven.docprocessing.dto.DocumentSummaryDto;
import com.suretyseven.docprocessing.dto.ExtractedResultDto;
import com.suretyseven.docprocessing.dto.HistoryEntryDto;
import com.suretyseven.docprocessing.dto.PagedResponse;
import com.suretyseven.docprocessing.dto.UploadResponse;
import com.suretyseven.docprocessing.entity.Document;
import com.suretyseven.docprocessing.entity.DocumentHistoryEntry;
import com.suretyseven.docprocessing.entity.DocumentStatus;
import com.suretyseven.docprocessing.entity.ExtractedResult;
import com.suretyseven.docprocessing.exception.DocumentNotFoundException;
import com.suretyseven.docprocessing.exception.InvalidUploadException;
import com.suretyseven.docprocessing.repository.DocumentHistoryRepository;
import com.suretyseven.docprocessing.repository.DocumentRepository;
import com.suretyseven.docprocessing.repository.ExtractedResultRepository;
import com.suretyseven.docprocessing.service.DocumentService;
import com.suretyseven.docprocessing.service.DuplicateDetectionService;
import com.suretyseven.docprocessing.service.FileStorageService;
import com.suretyseven.docprocessing.service.QueueService;
import com.suretyseven.docprocessing.util.HashUtils;
import com.suretyseven.docprocessing.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final DocumentRepository documentRepository;
    private final ExtractedResultRepository extractedResultRepository;
    private final DocumentHistoryRepository historyRepository;
    private final FileStorageService fileStorageService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final QueueService queueService;

    @Override
    @Transactional
    public UploadResponse upload(MultipartFile file, String documentType, String metadata) {
        if (file == null || file.isEmpty()) {
            throw new InvalidUploadException("A non-empty file is required");
        }
        if (documentType == null || documentType.isBlank()) {
            throw new InvalidUploadException("documentType is required");
        }

        byte[] content = fileStorageService.readBytes(file);
        String fileHash = HashUtils.sha256(content);
        String candidateId = generateUniqueId();

        Optional<String> existingId = duplicateDetectionService.reserve(fileHash, candidateId);
        if (existingId.isPresent()) {
            log.info("Duplicate upload detected, fileHash={}, existingDocumentId={}", fileHash, existingId.get());
            Document existing = documentRepository.findById(existingId.get())
                    .orElseGet(() -> documentRepository.findByFileHash(fileHash).orElse(null));
            String status = existing != null ? existing.getStatus().name() : DocumentStatus.UPLOADED.name();
            return UploadResponse.builder()
                    .documentId(existingId.get())
                    .status(status)
                    .duplicate(true)
                    .build();
        }

        String storedPath = fileStorageService.store(candidateId, file);
        Instant now = Instant.now();

        Document document = Document.builder()
                .id(candidateId)
                .originalFilename(file.getOriginalFilename())
                .storedPath(storedPath)
                .documentType(documentType)
                .metadata(metadata)
                .fileHash(fileHash)
                .status(DocumentStatus.UPLOADED)
                .retryCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        documentRepository.save(document);
        addHistory(candidateId, DocumentStatus.UPLOADED, null, null);

        queueService.enqueue(candidateId);
        log.info("Document uploaded and queued for processing, documentId={}", candidateId);

        return UploadResponse.builder()
                .documentId(candidateId)
                .status(DocumentStatus.UPLOADED.name())
                .duplicate(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDetailResponse getDocument(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        ExtractedResultDto resultDto = extractedResultRepository.findByDocumentId(documentId)
                .map(this::toDto)
                .orElse(null);

        List<String> validationErrors = extractedResultRepository.findByDocumentId(documentId)
                .map(ExtractedResult::getValidationErrors)
                .filter(errors -> errors != null && !errors.isBlank())
                .map(errors -> List.of(errors.split("\\|")))
                .orElse(null);

        return DocumentDetailResponse.builder()
                .documentId(document.getId())
                .status(document.getStatus().name())
                .documentType(document.getDocumentType())
                .originalFilename(document.getOriginalFilename())
                .failureReason(document.getFailureReason())
                .retryCount(document.getRetryCount())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .result(resultDto)
                .validationErrors(validationErrors)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoryEntryDto> getHistory(String documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }
        return historyRepository.findByDocumentIdOrderByTimestampAsc(documentId).stream()
                .map(entry -> HistoryEntryDto.builder()
                        .status(entry.getStatus().name())
                        .reason(entry.getReason())
                        .detail(entry.getDetail())
                        .timestamp(entry.getTimestamp())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DocumentSummaryDto> listDocuments(DocumentStatus status, String documentType, int page, int size) {
        Specification<Document> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (documentType != null && !documentType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("documentType"), documentType));
        }

        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        var resultPage = documentRepository.findAll(spec, pageable);

        List<DocumentSummaryDto> content = resultPage.getContent().stream()
                .map(doc -> DocumentSummaryDto.builder()
                        .documentId(doc.getId())
                        .filename(doc.getOriginalFilename())
                        .documentType(doc.getDocumentType())
                        .status(doc.getStatus().name())
                        .createdAt(doc.getCreatedAt())
                        .build())
                .toList();

        return PagedResponse.<DocumentSummaryDto>builder()
                .content(content)
                .page(resultPage.getNumber())
                .size(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .build();
    }

    private String generateUniqueId() {
        String id;
        int attempts = 0;
        do {
            id = IdGenerator.newDocumentId();
            attempts++;
        } while (documentRepository.existsById(id) && attempts < 5);
        return id;
    }

    private ExtractedResultDto toDto(ExtractedResult result) {
        return ExtractedResultDto.builder()
                .companyName(result.getCompanyName())
                .registrationNumber(result.getRegistrationNumber())
                .address(result.getAddress())
                .annualRevenue(result.getAnnualRevenue())
                .documentDate(result.getDocumentDate())
                .build();
    }

    private void addHistory(String documentId, DocumentStatus status, String reason, String detail) {
        historyRepository.save(DocumentHistoryEntry.builder()
                .documentId(documentId)
                .status(status)
                .reason(reason)
                .detail(detail)
                .timestamp(Instant.now())
                .build());
    }
}
