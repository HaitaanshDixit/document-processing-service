package com.suretyseven.docprocessing.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String store(String documentId, MultipartFile file);

    byte[] readBytes(MultipartFile file);
}
