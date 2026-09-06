package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.DateFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgDateColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The week/month/quarter/year relative options resolve against the zone in the date filter params,
 * which defaults to UTC, so their windows move with the run date and are anchored here to today in UTC.
 * Each row seeded here sits inside exactly one window
 * <em>by construction</em> (the first day of a period, or a whole period away from today), which makes
 * the assertions hold on any run date without re-deriving the adapter's boundary arithmetic. They assert
 * containment rather than exact result sets, because the standard 2024/2025 dataset drifts in and out of
 * the wider windows as the years pass. Day-based options live in {@link RelativeDateFilterTest}.
 */
class CalendarRelativeDateFilterTest extends ScenarioTestBase {

    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);
    private static final LocalDate FIRST_OF_MONTH = TODAY.withDayOfMonth(1);
    // quarter start taken from IsoFields rather than recomputed the way the adapter does
    private static final LocalDate FIRST_OF_QUARTER = TODAY
            .withMonth((TODAY.get(IsoFields.QUARTER_OF_YEAR) - 1) * 3 + 1)
            .withDayOfMonth(1);
    private static final LocalDate FIRST_OF_YEAR = TODAY.withDayOfYear(1);

    private static final long TODAY_ROW = 201L;
    private static final long LAST_WEEK = 202L;
    private static final long NEXT_WEEK = 203L;
    private static final long LAST_MONTH = 204L;
    private static final long NEXT_MONTH = 205L;
    private static final long LAST_QUARTER = 206L;
    private static final long NEXT_QUARTER = 207L;
    private static final long LAST_YEAR = 208L;
    private static final long NEXT_YEAR = 209L;
    private static final long MONTHS_AGO_5 = 210L;
    private static final long MONTHS_AGO_7 = 211L;
    private static final long MONTHS_AGO_13 = 212L;
    private static final long MONTHS_AGO_25 = 213L;

    @BeforeAll
    static void seedCalendarTrades() {
        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();
        em.persist(calendarTrade(TODAY_ROW, TODAY));
        // exactly one week away lands in the neighbouring week whatever weekday today is
        em.persist(calendarTrade(LAST_WEEK, TODAY.minusDays(7)));
        em.persist(calendarTrade(NEXT_WEEK, TODAY.plusDays(7)));
        em.persist(calendarTrade(LAST_MONTH, FIRST_OF_MONTH.minusMonths(1)));
        em.persist(calendarTrade(NEXT_MONTH, FIRST_OF_MONTH.plusMonths(1)));
        // one month before the quarter start is the last month of the previous quarter
        em.persist(calendarTrade(LAST_QUARTER, FIRST_OF_QUARTER.minusMonths(1)));
        em.persist(calendarTrade(NEXT_QUARTER, FIRST_OF_QUARTER.plusMonths(3)));
        em.persist(calendarTrade(LAST_YEAR, FIRST_OF_YEAR.minusYears(1)));
        em.persist(calendarTrade(NEXT_YEAR, FIRST_OF_YEAR.plusYears(1)));
        em.persist(calendarTrade(MONTHS_AGO_5, TODAY.minusMonths(5)));
        em.persist(calendarTrade(MONTHS_AGO_7, TODAY.minusMonths(7)));
        em.persist(calendarTrade(MONTHS_AGO_13, TODAY.minusMonths(13)));
        em.persist(calendarTrade(MONTHS_AGO_25, TODAY.minusMonths(25)));
        em.getTransaction().commit();
        em.close();
    }

    private static Trade calendarTrade(Long id, LocalDate tradeDate) {
        return new Trade(id, "Calendar", null, null, BigDecimal.ONE, null, tradeDate, null, null, null, null, null);
    }

    private List<Long> rows(String relativeOption) {
        return rows(relativeOption, DateFilterParams.builder().filterOptions(SimpleFilterModelType.values()).build());
    }

    private List<Long> rows(String relativeOption, DateFilterParams params) {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.tradeDate).filter(AgDateColumnFilter.forLocalDate().filterParams(params)).build()
                )
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("tradeDate", filter(relativeOption)));
        return tradeIds(queryBuilder.getRows(request));
    }

    @Test
    void thisWeek() {
        assertThat(rows("thisWeek")).contains(TODAY_ROW).doesNotContain(LAST_WEEK, NEXT_WEEK);
    }

    @Test
    void lastWeek() {
        assertThat(rows("lastWeek")).contains(LAST_WEEK).doesNotContain(TODAY_ROW, NEXT_WEEK);
    }

    @Test
    void nextWeek() {
        assertThat(rows("nextWeek")).contains(NEXT_WEEK).doesNotContain(TODAY_ROW, LAST_WEEK);
    }

    @Test
    void thisMonth() {
        assertThat(rows("thisMonth")).contains(TODAY_ROW).doesNotContain(LAST_MONTH, NEXT_MONTH);
    }

    @Test
    void lastMonth() {
        assertThat(rows("lastMonth")).contains(LAST_MONTH).doesNotContain(TODAY_ROW, NEXT_MONTH);
    }

    @Test
    void nextMonth() {
        assertThat(rows("nextMonth")).contains(NEXT_MONTH).doesNotContain(TODAY_ROW, LAST_MONTH);
    }

    @Test
    void thisQuarter() {
        assertThat(rows("thisQuarter")).contains(TODAY_ROW).doesNotContain(LAST_QUARTER, NEXT_QUARTER);
    }

    @Test
    void lastQuarter() {
        assertThat(rows("lastQuarter")).contains(LAST_QUARTER).doesNotContain(TODAY_ROW, NEXT_QUARTER);
    }

    @Test
    void nextQuarter() {
        assertThat(rows("nextQuarter")).contains(NEXT_QUARTER).doesNotContain(TODAY_ROW, LAST_QUARTER);
    }

    @Test
    void thisYear() {
        assertThat(rows("thisYear")).contains(TODAY_ROW).doesNotContain(LAST_YEAR, NEXT_YEAR);
    }

    @Test
    void lastYear() {
        assertThat(rows("lastYear")).contains(LAST_YEAR).doesNotContain(TODAY_ROW, NEXT_YEAR);
    }

    @Test
    void nextYear() {
        assertThat(rows("nextYear")).contains(NEXT_YEAR).doesNotContain(TODAY_ROW, LAST_YEAR);
    }

    @Test
    void yearToDate() {
        // window ends after today, so next week is out even when it stays inside this year
        assertThat(rows("yearToDate")).contains(TODAY_ROW).doesNotContain(NEXT_WEEK, LAST_YEAR, NEXT_YEAR);
    }

    @Test
    void last6Months() {
        assertThat(rows("last6Months")).contains(TODAY_ROW, MONTHS_AGO_5).doesNotContain(MONTHS_AGO_7);
    }

    @Test
    void last12Months() {
        assertThat(rows("last12Months")).contains(MONTHS_AGO_7).doesNotContain(MONTHS_AGO_13);
    }

    @Test
    void last24Months() {
        assertThat(rows("last24Months")).contains(MONTHS_AGO_13).doesNotContain(MONTHS_AGO_25);
    }

    @Test
    void includeBlanksInRangeParamAddsNullDatesToRelativeOptions() {
        List<Long> ids = rows("thisYear", DateFilterParams.builder()
                .filterOptions(SimpleFilterModelType.values())
                .includeBlanksInRange(true)
                .build());
        // trade 8 of the standard dataset has a null tradeDate
        assertThat(ids).contains(TODAY_ROW, 8L);
    }
}
