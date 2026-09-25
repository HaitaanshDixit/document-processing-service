package com.suretyseven.docprocessing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    private String documentId;
    private String status;
    private boolean duplicate;
}
