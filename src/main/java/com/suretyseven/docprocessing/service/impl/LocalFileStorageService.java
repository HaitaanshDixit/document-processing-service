package com.suretyseven.docprocessing.service.impl;

import com.suretyseven.docprocessing.exception.InvalidUploadException;
import com.suretyseven.docprocessing.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadDir;

    public LocalFileStorageService(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir);
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to initialize upload directory", e);
        }
    }

    @Override
    public String store(String documentId, MultipartFile file) {
        String safeFilename = sanitize(file.getOriginalFilename());
        Path target = uploadDir.resolve(documentId + "_" + safeFilename);
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }
        return target.toString();
    }

    @Override
    public byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new InvalidUploadException("Unable to read uploaded file");
        }
    }

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "upload.bin";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
