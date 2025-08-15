package com.practice.apiservice.batch.processor;

import java.util.List;

public class RecordValidationException extends RuntimeException {
    private final long rowNumber;
    private final List<FieldError> errors;

    public RecordValidationException(long rowNumber, List<FieldError> errors) {
        super(buildMessage(rowNumber, errors));
        this.rowNumber = rowNumber;
        this.errors = errors;
    }

    public long getRowNumber() { return rowNumber; }
    public List<FieldError> getErrors() { return errors; }

    public static final class FieldError {
        private final String field;
        private final String reason;
        public FieldError(String field, String reason) { this.field = field; this.reason = reason; }
        public String getField()  { return field; }
        public String getReason() { return reason; }
        @Override public String toString() { return field + "=" + reason; }
    }

    private static String buildMessage(long row, List<FieldError> errs) {
        return "row=" + row + " errors=" + errs;
    }
}
