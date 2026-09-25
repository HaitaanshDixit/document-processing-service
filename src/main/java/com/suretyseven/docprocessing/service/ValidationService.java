package com.suretyseven.docprocessing.service;

import com.suretyseven.docprocessing.dto.ExtractedResultDto;

import java.util.List;

public interface ValidationService {

    List<String> validate(ExtractedResultDto data);
}
