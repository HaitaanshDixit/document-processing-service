package com.suretyseven.docprocessing.entity;

public final class FailureReasons {

    public static final String PROCESSOR_TIMEOUT = "PROCESSOR_TIMEOUT";
    public static final String PROCESSOR_ERROR = "PROCESSOR_ERROR";
    public static final String INVALID_EXTRACTION_RESULT = "INVALID_EXTRACTION_RESULT";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String MAX_RETRIES_EXCEEDED = "MAX_RETRIES_EXCEEDED";
    public static final String REQUEUED_AFTER_CRASH_RECOVERY = "REQUEUED_AFTER_CRASH_RECOVERY";

    private FailureReasons() {
    }
}
