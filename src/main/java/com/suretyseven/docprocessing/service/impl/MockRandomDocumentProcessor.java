package com.suretyseven.docprocessing.service.impl;

import com.suretyseven.docprocessing.dto.ExtractedResultDto;
import com.suretyseven.docprocessing.entity.Document;
import com.suretyseven.docprocessing.entity.ProcessorOutcomeType;
import com.suretyseven.docprocessing.service.DocumentProcessor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;

@Service
public class MockRandomDocumentProcessor implements DocumentProcessor {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final List<String> COMPANY_NAMES = List.of(
            "ABC Construction Pvt Ltd",
            "Vertex Infra Ltd",
            "Bluepeak Traders Pvt Ltd",
            "Northstar Logistics Ltd",
            "Greenfield Agro Pvt Ltd"
    );

    private static final List<String> ADDRESSES = List.of(
            "New Delhi", "Mumbai", "Bengaluru", "Jaipur", "Pune"
    );

    @Override
    public ProcessorOutcome process(Document document) {
        int roll = RANDOM.nextInt(100);

        if (roll < 60) {
            return new ProcessorOutcome(ProcessorOutcomeType.SUCCESS, generateData());
        } else if (roll < 75) {
            return new ProcessorOutcome(ProcessorOutcomeType.TIMEOUT, null);
        } else if (roll < 90) {
            return new ProcessorOutcome(ProcessorOutcomeType.ERROR, null);
        } else {
            return new ProcessorOutcome(ProcessorOutcomeType.INVALID_RESULT, null);
        }
    }

    private ExtractedResultDto generateData() {
        boolean introduceInvalidData = RANDOM.nextInt(100) < 15;

        String companyName = introduceInvalidData && RANDOM.nextBoolean()
                ? null
                : COMPANY_NAMES.get(RANDOM.nextInt(COMPANY_NAMES.size()));

        BigDecimal revenue = introduceInvalidData && RANDOM.nextBoolean()
                ? BigDecimal.valueOf(-1)
                : BigDecimal.valueOf(1_000_000L + RANDOM.nextInt(50_000_000));

        LocalDate documentDate = LocalDate.now().minusDays(RANDOM.nextInt(730));

        return ExtractedResultDto.builder()
                .companyName(companyName)
                .registrationNumber("U" + (10000 + RANDOM.nextInt(89999)) + "DL2020PTC" + (100000 + RANDOM.nextInt(899999)))
                .address(ADDRESSES.get(RANDOM.nextInt(ADDRESSES.size())))
                .annualRevenue(revenue)
                .documentDate(documentDate)
                .build();
    }
}
