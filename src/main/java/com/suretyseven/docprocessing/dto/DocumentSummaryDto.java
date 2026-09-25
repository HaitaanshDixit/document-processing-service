package com.suretyseven.docprocessing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSummaryDto {
    private String documentId;
    private String filename;
    private String documentType;
    private String status;
    private Instant createdAt;
}
