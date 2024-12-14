package com.aggrid.jpa.adapter.utils;


import jakarta.persistence.criteria.Expression;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.List;

public class TypeValueSynchronizer {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ISO_DATE_TIME,                   // "1999-12-31T23:00:00"
            DateTimeFormatter.ISO_INSTANT,                     // "1999-12-31T23:00:00Z"
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,             // "1999-12-31T23:00:00.838"
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),// "1999-12-31 23:00:00"
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),// "1999/12/31 23:00:00"
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),         // "1999-12-31"
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss") // "31-12-1999 23:00:00"
    );
    
    public static Result synchronizeTypes(Expression<?> expr, Object value) {
        Class<?> exprType = expr.getJavaType();
        Class<?> valueType = value.getClass();
        
        // compatible
        if (exprType.isAssignableFrom(valueType)) {
            return new Result(expr, value);
        }

        // date type
        if (Date.class.isAssignableFrom(exprType) || Temporal.class.isAssignableFrom(exprType)) {
            return new Result(
                    expr.as(LocalDateTime.class),
                    parseToLocalDateTime(value.toString())
            );
        }
        
        // number type
        if (Number.class.isAssignableFrom(exprType)) {
            // can make them both big decimal and comparable
            return new Result(
                    expr.as(BigDecimal.class),
                    new BigDecimal(value.toString())
            );
        }

        // boolean
        if (exprType.equals(Boolean.class)) {
            return new Result(
                    expr,
                    Boolean.parseBoolean(value.toString())
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
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDateTime.parse(dateTimeStr, formatter);
            } catch (DateTimeParseException ignored) {
                // Continue to the next formatter
            }
        }

        throw new DateTimeParseException("Unable to parse date-time string", dateTimeStr, 0);
    }
    
    public static class Result {
        private final Expression<?> synchronizedPath;
        private final Object synchronizedValue;
        
        public Result(Expression<?> synchronizedPath, Object synchronizedValue) {
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
