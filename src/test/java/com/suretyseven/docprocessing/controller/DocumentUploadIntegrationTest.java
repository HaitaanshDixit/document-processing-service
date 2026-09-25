package com.suretyseven.docprocessing.controller;

import com.suretyseven.docprocessing.repository.DocumentRepository;
import com.suretyseven.docprocessing.support.TestSupportConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSupportConfig.class)
class DocumentUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Test
    void validUpload_returnsCreatedWithUploadedStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "statement.pdf", "application/pdf", "content-a".getBytes());

        mockMvc.perform(multipart("/documents").file(file).param("documentType", "FINANCIAL_STATEMENT"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("UPLOADED")))
                .andExpect(jsonPath("$.duplicate", is(false)));

        assertThat(documentRepository.count()).isEqualTo(1);
    }

    @Test
    void duplicateUpload_returnsSameDocumentIdAndDoesNotCreateSecondRecord() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "statement.pdf", "application/pdf", "same-bytes".getBytes());

        String firstResponse = mockMvc.perform(multipart("/documents").file(file).param("documentType", "FINANCIAL_STATEMENT"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String documentId = com.jayway.jsonpath.JsonPath.read(firstResponse, "$.documentId");

        MockMultipartFile sameFileAgain = new MockMultipartFile("file", "statement-renamed.pdf", "application/pdf", "same-bytes".getBytes());

        mockMvc.perform(multipart("/documents").file(sameFileAgain).param("documentType", "FINANCIAL_STATEMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId", is(documentId)))
                .andExpect(jsonPath("$.duplicate", is(true)));

        assertThat(documentRepository.count()).isEqualTo(1);
    }
}
