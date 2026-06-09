package io.github.smolcan.aggrid.jpa.adapter.exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when a ServerSideGetRowsRequest contains invalid or malformed data.
 * This is a runtime exception that provides structured information about validation failures.
 */
@Getter
public class InvalidRequestException extends RuntimeException {

    private final List<ValidationError> validationErrors;

    public InvalidRequestException(List<ValidationError> validationErrors) {
        super(formatErrorMessage(validationErrors));
        this.validationErrors = new ArrayList<>(validationErrors);
    }

    public InvalidRequestException(String field, String message) {
        this(List.of(new ValidationError(field, message)));
    }

    public boolean hasErrorsForField(String field) {
        return validationErrors.stream()
                .anyMatch(error -> field.equals(error.getField()));
    }

    private static String formatErrorMessage(List<ValidationError> errors) {
        if (errors.isEmpty()) {
            return "Request validation failed";
        }

        StringBuilder sb = new StringBuilder("Request validation failed:\n");
        for (ValidationError error : errors) {
            sb.append("- ").append(error.getField()).append(": ").append(error.getMessage()).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Represents a single field validation error
     */
    @Getter
    @RequiredArgsConstructor
    @ToString
    public static class ValidationError {
        private final String field;
        private final String message;
        private final Object rejectedValue;

        public ValidationError(String field, String message) {
            this(field, message, null);
        }
    }
}
