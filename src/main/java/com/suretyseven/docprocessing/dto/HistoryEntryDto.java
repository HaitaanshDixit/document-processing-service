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
public class HistoryEntryDto {
    private String status;
    private String reason;
    private String detail;
    private Instant timestamp;
}
