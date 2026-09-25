package com.suretyseven.docprocessing.service;

import com.suretyseven.docprocessing.dto.DocumentDetailResponse;
import com.suretyseven.docprocessing.dto.DocumentSummaryDto;
import com.suretyseven.docprocessing.dto.HistoryEntryDto;
import com.suretyseven.docprocessing.dto.PagedResponse;
import com.suretyseven.docprocessing.dto.UploadResponse;
import com.suretyseven.docprocessing.entity.DocumentStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    UploadResponse upload(MultipartFile file, String documentType, String metadata);

    DocumentDetailResponse getDocument(String documentId);

    List<HistoryEntryDto> getHistory(String documentId);

    PagedResponse<DocumentSummaryDto> listDocuments(DocumentStatus status, String documentType, int page, int size);
}
