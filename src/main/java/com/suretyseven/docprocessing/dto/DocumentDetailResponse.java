package com.suretyseven.docprocessing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDetailResponse {
    private String documentId;
    private String status;
    private String documentType;
    private String originalFilename;
    private String failureReason;
    private int retryCount;
    private Instant createdAt;
    private Instant updatedAt;
    private ExtractedResultDto result;
    private List<String> validationErrors;
}
