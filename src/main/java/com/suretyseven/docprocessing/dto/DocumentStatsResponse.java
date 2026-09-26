package com.suretyseven.docprocessing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStatsResponse {
    private long uploaded;
    private long processing;
    private long processed;
    private long failed;
    private long total;
}