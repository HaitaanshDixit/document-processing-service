package com.suretyseven.docprocessing.service.impl;

import com.suretyseven.docprocessing.dto.ExtractedResultDto;
import com.suretyseven.docprocessing.service.ValidationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExtractedDataValidationService implements ValidationService {

    @Override
    public List<String> validate(ExtractedResultDto data) {
        List<String> errors = new ArrayList<>();

        if (data == null) {
            errors.add("Extraction returned no data");
            return errors;
        }

        if (isBlank(data.getCompanyName())) {
            errors.add("companyName is required");
        }

        if (isBlank(data.getRegistrationNumber())) {
            errors.add("registrationNumber is required");
        }

        BigDecimal revenue = data.getAnnualRevenue();
        if (revenue == null) {
            errors.add("annualRevenue is required");
        } else if (revenue.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("annualRevenue must be greater than or equal to 0");
        }

        LocalDate documentDate = data.getDocumentDate();
        if (documentDate == null) {
            errors.add("documentDate is required and must be a valid date");
        } else if (documentDate.isAfter(LocalDate.now())) {
            errors.add("documentDate cannot be in the future");
        }

        return errors;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
