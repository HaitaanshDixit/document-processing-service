package com.suretyseven.docprocessing.controller;

import com.suretyseven.docprocessing.dto.*;
import com.suretyseven.docprocessing.entity.DocumentStatus;
import com.suretyseven.docprocessing.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/documents", consumes = "multipart/form-data")
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "metadata", required = false) String metadata) {
        UploadResponse response = documentService.upload(file, documentType, metadata);
        HttpStatus status = response.isDuplicate() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/documents/stats")
    public ResponseEntity<DocumentStatsResponse> getStats() {
        return ResponseEntity.ok(documentService.getStats());
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<DocumentDetailResponse> getDocument(@PathVariable String documentId) {
        return ResponseEntity.ok(documentService.getDocument(documentId));
    }

    @GetMapping("/documents/{documentId}/history")
    public ResponseEntity<List<HistoryEntryDto>> getHistory(@PathVariable String documentId) {
        return ResponseEntity.ok(documentService.getHistory(documentId));
    }

    @GetMapping("/documents")
    public ResponseEntity<PagedResponse<DocumentSummaryDto>> listDocuments(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String documentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(documentService.listDocuments(status, documentType, page, size));
    }
}
