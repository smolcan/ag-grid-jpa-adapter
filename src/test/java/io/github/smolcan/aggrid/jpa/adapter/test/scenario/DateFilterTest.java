package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.DateFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgDateColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateFilterTest extends ScenarioTestBase {

    private LoadSuccessParams rows(Map<String, Object> filterModel) {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(filterModel);
        return defaultQueryBuilder().getRows(request);
    }

    @Test
    void equalsOnLocalDate() {
        LoadSuccessParams result = rows(Map.of("tradeDate", dateFilter("equals", "2024-05-05 00:00:00")));
        assertThat(tradeIds(result)).containsExactly(5L);
    }

    @Test
    void lessThanOnLocalDate() {
        LoadSuccessParams result = rows(Map.of("tradeDate", dateFilter("lessThan", "2024-04-01 00:00:00")));
        // strictly before Apr 1; the Apr 1 trade (4) and null date (8) excluded
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 3L);
    }

    @Test
    void greaterThanOnLocalDate() {
        LoadSuccessParams result = rows(Map.of("tradeDate", dateFilter("greaterThan", "2024-12-31 00:00:00")));
        assertThat(tradeIds(result)).containsExactly(9L, 10L, 11L, 12L);
    }

    @Test
    void inRangeIsExclusiveByDefault() {
        LoadSuccessParams result = rows(Map.of(
                "tradeDate", dateRangeFilter("2024-02-01 00:00:00", "2024-05-05 00:00:00")
        ));
        // strictly between Feb 1 and May 5: Feb 15 (2), Mar 20 (3), Apr 1 (4)
        assertThat(tradeIds(result)).containsExactly(2L, 3L, 4L);
    }

    @Test
    void blankMatchesNullDate() {
        LoadSuccessParams result = rows(Map.of("tradeDate", filter("blank")));
        assertThat(tradeIds(result)).containsExactly(8L);
    }

    @Test
    void notBlankExcludesNullDate() {
        LoadSuccessParams result = rows(Map.of("tradeDate", filter("notBlank")));
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 9L, 10L, 11L, 12L);
    }

    @Test
    void equalsOnLocalDateTimeMatchesExactSecond() {
        LoadSuccessParams result = rows(Map.of("submittedAt", dateFilter("equals", "2024-02-15 10:30:00")));
        assertThat(tradeIds(result)).containsExactly(2L);
    }

    @Test
    void greaterThanOrEqualOnLocalDateTime() {
        LoadSuccessParams result = rows(Map.of("submittedAt", dateFilter("greaterThanOrEqual", "2025-01-01 08:00:00")));
        assertThat(tradeIds(result)).containsExactly(9L, 10L, 11L, 12L);
    }

    @Test
    void combinedConditionsWithOr() {
        LoadSuccessParams result = rows(Map.of(
                "tradeDate", combined("OR",
                        dateFilter("lessThan", "2024-03-01 00:00:00"),
                        dateFilter("greaterThan", "2025-03-31 00:00:00"))
        ));
        // before Mar 2024 (1, 2) or after Mar 2025 (12)
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 12L);
    }

    @Test
    void minValidYearRejectsAncientDates() {
        // DateFilterParams defaults minValidYear to 1000
        assertThatThrownBy(() -> rows(Map.of("tradeDate", dateFilter("equals", "0900-01-01 00:00:00"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private QueryBuilder<Trade, Long, Void> tradeDateQueryBuilder(DateFilterParams params) {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.tradeDate).filter(AgDateColumnFilter.forLocalDate().filterParams(params)).build()
                )
                .build();
    }

    private List<Long> tradeDateRows(DateFilterParams params, Map<String, Object> filterModel) {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("tradeDate", filterModel));
        return tradeIds(tradeDateQueryBuilder(params).getRows(request));
    }

    @Test
    void inRangeInclusiveParamIncludesEndpointsAndBlanks() {
        List<Long> ids = tradeDateRows(
                DateFilterParams.builder().inRangeInclusive(true).includeBlanksInRange(true).build(),
                dateRangeFilter("2024-02-15 00:00:00", "2024-05-05 00:00:00"));
        // endpoints (2, 5) included, plus the null date (8)
        assertThat(ids).containsExactly(2L, 3L, 4L, 5L, 8L);
    }

    @Test
    void includeBlanksInEqualsParamAddsNullDates() {
        List<Long> ids = tradeDateRows(
                DateFilterParams.builder().includeBlanksInEquals(true).build(),
                dateFilter("equals", "2024-01-10 00:00:00"));
        assertThat(ids).containsExactly(1L, 8L);
    }

    @Test
    void includeBlanksInLessThanParamAddsNullDates() {
        List<Long> ids = tradeDateRows(
                DateFilterParams.builder().includeBlanksInLessThan(true).build(),
                dateFilter("lessThan", "2024-04-01 00:00:00"));
        assertThat(ids).containsExactly(1L, 2L, 3L, 8L);
    }

    @Test
    void includeBlanksInNotEqualParamAddsNullDates() {
        List<Long> ids = tradeDateRows(
                DateFilterParams.builder().includeBlanksInNotEqual(true).build(),
                dateFilter("notEqual", "2024-01-10 00:00:00"));
        // SQL "date <> ..." is unknown for the null date (8), so only the param brings it back
        assertThat(ids).containsExactly(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
    }

    @Test
    void includeBlanksInLessThanParamAlsoCoversLessThanOrEqual() {
        List<Long> ids = tradeDateRows(
                DateFilterParams.builder().includeBlanksInLessThan(true).build(),
                dateFilter("lessThanOrEqual", "2024-04-01 00:00:00"));
        // same param as lessThan, and Apr 1 (4) is now included
        assertThat(ids).containsExactly(1L, 2L, 3L, 4L, 8L);
    }

    @Test
    void includeBlanksInGreaterThanParamAddsNullDates() {
        List<Long> ids = tradeDateRows(
                DateFilterParams.builder().includeBlanksInGreaterThan(true).build(),
                dateFilter("greaterThan", "2025-01-01 00:00:00"));
        // strictly after Jan 1 2025, so the Jan 1 trade (9) stays out while the null date (8) comes in
        assertThat(ids).containsExactly(8L, 10L, 11L, 12L);
    }

    @Test
    void includeBlanksInGreaterThanParamAlsoCoversGreaterThanOrEqual() {
        List<Long> ids = tradeDateRows(
                DateFilterParams.builder().includeBlanksInGreaterThan(true).build(),
                dateFilter("greaterThanOrEqual", "2025-01-01 00:00:00"));
        assertThat(ids).containsExactly(8L, 9L, 10L, 11L, 12L);
    }

    @Test
    void textFilterTypeOnDateColumnIsRejected() {
        // "contains" parses as a filter model type but has no meaning for dates
        assertThatThrownBy(() -> rows(Map.of("tradeDate", filter("contains"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contains");
    }

    @Test
    void maxValidYearRejectsLaterDates() {
        assertThatThrownBy(() -> tradeDateRows(
                DateFilterParams.builder().maxValidYear(2024).build(),
                dateFilter("equals", "2025-01-01 00:00:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Max valid year");
    }

    @Test
    void minValidDateOverridesMinValidYear() {
        assertThatThrownBy(() -> tradeDateRows(
                DateFilterParams.builder().minValidDate(LocalDate.of(2024, 6, 1)).build(),
                dateFilter("equals", "2024-01-10 00:00:00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Min valid date");
    }

    @Test
    void relativeDateOptionRequiresEnablingInFilterOptions() {
        // default DateFilterParams has empty filterOptions -> relative filters rejected
        assertThatThrownBy(() -> rows(Map.of("tradeDate", filter("today"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("filter options");
    }
}
