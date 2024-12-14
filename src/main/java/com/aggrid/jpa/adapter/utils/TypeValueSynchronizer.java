package com.aggrid.jpa.adapter.utils;


import jakarta.persistence.criteria.Expression;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.List;


/**
 * Before comparing expression with value in criteria API, need to make sure they are compatible and minimize runtime errors
 */
public class TypeValueSynchronizer {

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_DATE_TIME,                   // "1999-12-31T23:00:00"
            DateTimeFormatter.ISO_INSTANT,                     // "1999-12-31T23:00:00Z"
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,             // "1999-12-31T23:00:00.838"
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),// "1999-12-31 23:00:00"
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),// "1999/12/31 23:00:00"
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),         // "1999-12-31"
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),// "31-12-1999 23:00:00"
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    );


    /**
     * Synchronizes the types of the expression and value provided for a query,
     * ensuring that the value is converted to the appropriate type for comparison
     * based on the expression's expected type.
     * This method handles a variety of data types including:
     * <ul>
     *     <li>String</li>
     *     <li>Date (java.util.Date)</li>
     *     <li>Temporal types (LocalDateTime, Instant, LocalDate, ZonedDateTime)</li>
     *     <li>Number types (converted to BigDecimal)</li>
     *     <li>Boolean types</li>
     * </ul>
     *
     * The method returns a Result object containing the converted expression and value.
     * The value is parsed from the String representation into the appropriate type
     * based on the type of the expression.
     * If the type of the expression is unsupported, an {@link IllegalArgumentException}
     * is thrown.
     *
     * @param expr The expression to be synchronized, representing the expected type.
     * @param value The String value to be converted to the appropriate type for comparison.
     * @return A Result object containing the expression and the converted value, both of which are compatible.
     * @throws IllegalArgumentException if the type of the expression is not supported or if parsing fails.
     */
    public static Result<?> synchronizeTypes(Expression<?> expr, String value) {
        Class<?> exprType = expr.getJavaType();
        
        // compatible
        if (exprType.equals(String.class)) {
            return new Result<>(expr.as(String.class), value);
        }

        // date type
        if (Date.class.isAssignableFrom(exprType)) {
            return new Result<>(
                    expr.as(Date.class),
                    parseToDate(value)
            );
        } else if (Temporal.class.isAssignableFrom(exprType)) {
            
            if (exprType.equals(LocalDateTime.class)) {
                return new Result<>(
                        expr.as(LocalDateTime.class),
                        parseToLocalDateTime(value)
                );
            } else if (exprType.equals(Instant.class)) {
                return new Result<>(
                        expr.as(Instant.class),
                        parseToInstant(value)
                );
            } else if (exprType.equals(LocalDate.class)) {
                return new Result<>(
                        expr.as(LocalDate.class),
                        parseToLocalDate(value)
                );
            } else if (exprType.isAssignableFrom(ZonedDateTime.class)) {
                return new Result<>(
                        expr.as(ZonedDateTime.class),
                        parseToZonedDateTime(value)
                );
            } else {
                throw new IllegalArgumentException("Unsupported type " + exprType);
            }
        }
        
        // number type
        if (Number.class.isAssignableFrom(exprType)) {
            // can make them both big decimal and comparable
            return new Result<>(
                    expr.as(BigDecimal.class),
                    new BigDecimal(value)
            );
        }

        // boolean
        if (exprType.equals(Boolean.class)) {
            return new Result<>(
                    expr.as(Boolean.class),
                    Boolean.parseBoolean(value)
            );
        }

        // idk wtf this field is, compare without check, universe might explode
        return new Result(expr, value);
    }


    private static LocalDateTime parseToLocalDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            throw new IllegalArgumentException("Date string cannot be null or blank");
        }

        // 1. Handle ISO-8601 strings with "Z" or time zone offsets
        try {
            Instant instant = Instant.parse(dateTimeStr); // Parses strings like "2000-01-02T00:20:41.396Z"
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // Not an Instant, move to other patterns
        }

        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_ZONED_DATE_TIME);
            return zonedDateTime.toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // Not a ZonedDateTime, move on
        }

        // 2. Attempt parsing with other common formats
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateTimeStr, formatter);
            } catch (DateTimeParseException ignored) {
                // Continue to the next formatter
            }
        }

        throw new DateTimeParseException("Unable to parse date-time string", dateTimeStr, 0);
    }

    // Method to parse String to java.util.Date
    private static Date parseToDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("Date string cannot be null or blank");
        }

        // 1. Try parsing as ISO 8601 (Instant format)
        try {
            Instant instant = Instant.parse(dateStr); // Parses strings like "2000-01-02T00:20:41.396Z"
            return Date.from(instant);
        } catch (DateTimeParseException ignored) {
            // Not an Instant, continue to other formats
        }

        // 2. Try parsing using SimpleDateFormat (for common formats)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return sdf.parse(dateStr);  // Example: "1999-12-31 23:00:00"
        } catch (ParseException ignored) {
            // Not a matching format, move to the next
        }

        sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        try {
            return sdf.parse(dateStr);  // Example: "1999/12/31 23:00:00"
        } catch (ParseException ignored) {
            // Not a matching format, move to the next
        }

        throw new IllegalArgumentException("Unable to parse date string: " + dateStr);
    }

    private static LocalDate parseToLocalDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("Date string cannot be null or blank");
        }

        // 1. Try parsing as ISO 8601 LocalDate
        try {
            return LocalDate.parse(dateStr);  // Example: "1999-12-31"
        } catch (DateTimeParseException ignored) {
            // Not an ISO LocalDate, continue to other formats
        }

        // 2. Try parsing with common formats (e.g., "yyyy-MM-dd")
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
                // Continue to the next formatter
            }
        }

        throw new DateTimeParseException("Unable to parse date string", dateStr, 0);
    }

    // Method to parse String to Instant
    private static Instant parseToInstant(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("Date string cannot be null or blank");
        }

        // 1. Try parsing as ISO 8601 Instant
        try {
            return Instant.parse(dateStr); // Example: "1999-12-31T23:00:00Z"
        } catch (DateTimeParseException ignored) {
            // Not an Instant, continue to other formats
        }

        // 2. Handle other custom formats for Instant (e.g., using SimpleDateFormat)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return sdf.parse(dateStr).toInstant(); // Convert parsed Date to Instant
        } catch (ParseException ignored) {
            // Not a matching format, continue
        }

        throw new IllegalArgumentException("Unable to parse instant string: " + dateStr);
    }

    // Method to parse String to ZonedDateTime
    private static ZonedDateTime parseToZonedDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("Date string cannot be null or blank");
        }

        // 1. Try parsing as ISO 8601 ZonedDateTime
        try {
            return ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_ZONED_DATE_TIME);  // Example: "1999-12-31T23:00:00+01:00"
        } catch (DateTimeParseException ignored) {
            // Not a ZonedDateTime, move to the next
        }

        // 2. Handle other formats as needed
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");  // Example format: "1999-12-31 23:00:00 +0100"
        try {
            return ZonedDateTime.parse(dateStr, formatter);
        } catch (DateTimeParseException ignored) {
            // Not a matching format
        }

        throw new IllegalArgumentException("Unable to parse ZonedDateTime string: " + dateStr);
    }


    public static class Result<E> {
        private final Expression<E> synchronizedPath;
        private final E synchronizedValue;
        
        public Result(Expression<E> synchronizedPath, E synchronizedValue) {
            this.synchronizedPath = synchronizedPath;
            this.synchronizedValue = synchronizedValue;
        }

        public Expression<?> getSynchronizedPath() {
            return synchronizedPath;
        }

        public Object getSynchronizedValue() {
            return synchronizedValue;
        }
    }

}
