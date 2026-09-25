package com.suretyseven.docprocessing.exception;

public class RetryableProcessingException extends RuntimeException {
    public RetryableProcessingException(String reason) {
        super(reason);
    }
}
