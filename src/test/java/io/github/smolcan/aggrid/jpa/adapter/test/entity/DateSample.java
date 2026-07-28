package io.github.smolcan.aggrid.jpa.adapter.test.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Fixture entity carrying one column per date type supported by the AgDateColumnFilter
 * factories that Trade does not cover (Instant, OffsetDateTime, ZonedDateTime,
 * java.util.Date, java.sql.Timestamp, java.sql.Date).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class DateSample {

    @Id
    private Long sampleId;

    private Instant instantValue;

    private OffsetDateTime offsetDateTimeValue;

    private ZonedDateTime zonedDateTimeValue;

    @Temporal(TemporalType.TIMESTAMP)
    private Date utilDateValue;

    private Timestamp timestampValue;

    private java.sql.Date sqlDateValue;

    public DateSample(Long sampleId, Instant instantValue, OffsetDateTime offsetDateTimeValue, ZonedDateTime zonedDateTimeValue,
                      Date utilDateValue, Timestamp timestampValue, java.sql.Date sqlDateValue) {
        this.sampleId = sampleId;
        this.instantValue = instantValue;
        this.offsetDateTimeValue = offsetDateTimeValue;
        this.zonedDateTimeValue = zonedDateTimeValue;
        this.utilDateValue = utilDateValue;
        this.timestampValue = timestampValue;
        this.sqlDateValue = sqlDateValue;
    }
}
