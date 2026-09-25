package com.suretyseven.docprocessing.service;

import com.suretyseven.docprocessing.dto.ExtractedResultDto;
import com.suretyseven.docprocessing.entity.Document;
import com.suretyseven.docprocessing.entity.ProcessorOutcomeType;
import lombok.AllArgsConstructor;
import lombok.Getter;

public interface DocumentProcessor {

    ProcessorOutcome process(Document document);

    @Getter
    @AllArgsConstructor
    class ProcessorOutcome {
        private final ProcessorOutcomeType type;
        private final ExtractedResultDto data;
    }
}
