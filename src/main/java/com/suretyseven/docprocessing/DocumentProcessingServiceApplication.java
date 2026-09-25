package com.suretyseven.docprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DocumentProcessingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentProcessingServiceApplication.class, args);
    }
}
