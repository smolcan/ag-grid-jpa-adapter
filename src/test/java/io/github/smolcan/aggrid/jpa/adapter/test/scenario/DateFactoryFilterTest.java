package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.DateFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgDateColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortDirection;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.DateSample;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.DateSample_;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the AgDateColumnFilter factories for the date types Trade does not use.
 * All stored values and all filter conversions use the same fixed {@link #ZONE},
 * so the assertions hold regardless of the JVM's default time zone.
 * Samples 1-4 sit at 10:00 on consecutive days (2024-03-10 .. 2024-03-13), sample 5 is all-null.
 */
class DateFactoryFilterTest extends ScenarioTestBase {

    private static final ZoneId ZONE = ZoneId.of("Europe/Bratislava");
    // the zone now travels on the filter params instead of the factory; immutable, so one instance is shared
    private static final DateFilterParams ZONED_PARAMS = DateFilterParams.builder().zoneId(ZONE).build();
    private static final LocalDateTime BASE = LocalDateTime.of(2024, 3, 10, 10, 0, 0);

    @BeforeAll
    static void seedDateSamples() {
        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();
        for (long i = 1; i <= 4; i++) {
            LocalDateTime dateTime = BASE.plusDays(i - 1);
            ZonedDateTime zoned = dateTime.atZone(ZONE);
            em.persist(new DateSample(
                    i,
                    zoned.toInstant(),
                    zoned.toOffsetDateTime(),
                    zoned,
                    Date.from(zoned.toInstant()),
                    Timestamp.from(zoned.toInstant()),
                    java.sql.Date.valueOf(dateTime.toLocalDate())
            ));
        }
        em.persist(new DateSample(5L, null, null, null, null, null, null));
        em.getTransaction().commit();
        em.close();
    }

    private QueryBuilder<DateSample, Long, Void> dateSampleQueryBuilder() {
        return QueryBuilder.builder(DateSample.class, DateSample_.sampleId, entityManager)
                .colDefs(
                        ColDef.builder(DateSample_.sampleId).build(),
                        ColDef.builder(DateSample_.instantValue).filter(AgDateColumnFilter.forInstant().filterParams(ZONED_PARAMS)).build(),
                        ColDef.builder(DateSample_.offsetDateTimeValue).filter(AgDateColumnFilter.forOffsetDateTime().filterParams(ZONED_PARAMS)).build(),
                        ColDef.builder(DateSample_.zonedDateTimeValue).filter(AgDateColumnFilter.forZonedDateTime().filterParams(ZONED_PARAMS)).build(),
                        ColDef.builder(DateSample_.utilDateValue).filter(AgDateColumnFilter.forUtilDate().filterParams(ZONED_PARAMS)).build(),
                        ColDef.builder(DateSample_.timestampValue).filter(AgDateColumnFilter.forTimestamp().filterParams(ZONED_PARAMS)).build(),
                        ColDef.builder(DateSample_.sqlDateValue).filter(AgDateColumnFilter.forSqlDate()).build()
                )
                .build();
    }

    private List<Long> rows(String field, Map<String, Object> filterModel) {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getSortModel().add(sortItem("sampleId", SortDirection.asc));
        request.setFilterModel(Map.of(field, filterModel));

        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> row : dateSampleQueryBuilder().getRows(request).getRowData()) {
            ids.add(((Number) row.get("sampleId")).longValue());
        }
        return ids;
    }

    @Test
    void forInstant() {
        assertThat(rows("instantValue", dateFilter("equals", "2024-03-11 10:00:00"))).containsExactly(2L);
        assertThat(rows("instantValue", dateFilter("lessThan", "2024-03-12 10:00:00"))).containsExactly(1L, 2L);
        assertThat(rows("instantValue", filter("blank"))).containsExactly(5L);
    }

    @Test
    void forOffsetDateTime() {
        assertThat(rows("offsetDateTimeValue", dateFilter("equals", "2024-03-12 10:00:00"))).containsExactly(3L);
        assertThat(rows("offsetDateTimeValue", dateFilter("greaterThanOrEqual", "2024-03-12 10:00:00"))).containsExactly(3L, 4L);
        assertThat(rows("offsetDateTimeValue", filter("blank"))).containsExactly(5L);
    }

    @Test
    void forZonedDateTime() {
        assertThat(rows("zonedDateTimeValue", dateFilter("equals", "2024-03-13 10:00:00"))).containsExactly(4L);
        // exclusive range between day 1 and day 4
        assertThat(rows("zonedDateTimeValue", dateRangeFilter("2024-03-10 10:00:00", "2024-03-13 10:00:00"))).containsExactly(2L, 3L);
        assertThat(rows("zonedDateTimeValue", filter("notBlank"))).containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    void forUtilDate() {
        assertThat(rows("utilDateValue", dateFilter("equals", "2024-03-10 10:00:00"))).containsExactly(1L);
        assertThat(rows("utilDateValue", dateFilter("greaterThan", "2024-03-12 10:00:00"))).containsExactly(4L);
        assertThat(rows("utilDateValue", filter("blank"))).containsExactly(5L);
    }

    @Test
    void forTimestamp() {
        assertThat(rows("timestampValue", dateFilter("notEqual", "2024-03-10 10:00:00"))).containsExactly(2L, 3L, 4L);
        assertThat(rows("timestampValue", dateFilter("lessThanOrEqual", "2024-03-11 10:00:00"))).containsExactly(1L, 2L);
        assertThat(rows("timestampValue", filter("blank"))).containsExactly(5L);
    }

    @Test
    void forSqlDate() {
        // java.sql.Date is date-only: the filter's time part is dropped by the conversion
        assertThat(rows("sqlDateValue", dateFilter("equals", "2024-03-12 23:59:59"))).containsExactly(3L);
        assertThat(rows("sqlDateValue", dateFilter("lessThan", "2024-03-11 00:00:00"))).containsExactly(1L);
        assertThat(rows("sqlDateValue", filter("blank"))).containsExactly(5L);
    }
}
