package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.NumberFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgNumberColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NumberFilterTest extends ScenarioTestBase {

    private LoadSuccessParams rows(Map<String, Object> filterModel) {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(filterModel);
        return defaultQueryBuilder().getRows(request);
    }

    @Test
    void equalsMatchesAllRowsWithValue() {
        LoadSuccessParams result = rows(Map.of("currentValue", filter("equals", 100.00)));
        assertThat(tradeIds(result)).containsExactly(1L, 11L);
    }

    @Test
    void equalsOnIntegerColumn() {
        LoadSuccessParams result = rows(Map.of("submitterId", filter("equals", 101)));
        assertThat(tradeIds(result)).containsExactly(1L);
    }

    @Test
    void notEqualExcludesValueAndNulls() {
        LoadSuccessParams result = rows(Map.of("currentValue", filter("notEqual", 100.00)));
        assertThat(tradeIds(result)).containsExactly(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 12L);
    }

    @Test
    void lessThanIsStrict() {
        LoadSuccessParams result = rows(Map.of("currentValue", filter("lessThan", 0)));
        assertThat(tradeIds(result)).containsExactly(3L, 7L);
    }

    @Test
    void lessThanOrEqualIncludesBoundary() {
        LoadSuccessParams result = rows(Map.of("currentValue", filter("lessThanOrEqual", 0)));
        assertThat(tradeIds(result)).containsExactly(3L, 4L, 7L);
    }

    @Test
    void greaterThanIsStrict() {
        LoadSuccessParams result = rows(Map.of("currentValue", filter("greaterThan", 320.10)));
        assertThat(tradeIds(result)).containsExactly(5L, 10L);
    }

    @Test
    void greaterThanOrEqualIncludesBoundary() {
        LoadSuccessParams result = rows(Map.of("currentValue", filter("greaterThanOrEqual", 320.10)));
        assertThat(tradeIds(result)).containsExactly(5L, 6L, 10L);
    }

    @Test
    void inRangeIsExclusiveByDefault() {
        LoadSuccessParams result = rows(Map.of("currentValue", rangeFilter(42.42, 100.00)));
        // strictly between: only 75.25 (8)
        assertThat(tradeIds(result)).containsExactly(8L);
    }

    @Test
    void inRangeInclusiveParamIncludesEndpoints() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.currentValue)
                                .filter(new AgNumberColumnFilter<BigDecimal>().filterParams(NumberFilterParams.builder().inRangeInclusive(true).build()))
                                .build()
                )
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("currentValue", rangeFilter(42.42, 100.00)));

        LoadSuccessParams result = queryBuilder.getRows(request);
        // endpoints included: 42.42 (12), 75.25 (8), 100.00 (1, 11)
        assertThat(tradeIds(result)).containsExactly(1L, 8L, 11L, 12L);
    }

    @Test
    void blankMatchesOnlyNulls() {
        LoadSuccessParams result = rows(Map.of("previousValue", filter("blank")));
        assertThat(tradeIds(result)).containsExactly(2L, 6L, 12L);
    }

    @Test
    void notBlankExcludesNulls() {
        LoadSuccessParams result = rows(Map.of("previousValue", filter("notBlank")));
        assertThat(tradeIds(result)).containsExactly(1L, 3L, 4L, 5L, 7L, 8L, 9L, 10L, 11L);
    }

    @Test
    void nullsAreExcludedFromComparisonsByDefault() {
        LoadSuccessParams result = rows(Map.of("previousValue", filter("greaterThan", -1000)));
        // matches every non-null previousValue, nulls (2, 6, 12) excluded
        assertThat(tradeIds(result)).containsExactly(1L, 3L, 4L, 5L, 7L, 8L, 9L, 10L, 11L);
    }

    @Test
    void includeBlanksInEqualsParamAddsNulls() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.previousValue)
                                .filter(new AgNumberColumnFilter<Double>().filterParams(NumberFilterParams.builder().includeBlanksInEquals(true).build()))
                                .build()
                )
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("previousValue", filter("equals", 90.0)));

        LoadSuccessParams result = queryBuilder.getRows(request);
        // exact match (1) plus null previousValue (2, 6, 12)
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 6L, 12L);
    }

    private QueryBuilder<Trade, Long, Void> previousValueQueryBuilder(NumberFilterParams params) {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.previousValue).filter(new AgNumberColumnFilter<Double>().filterParams(params)).build()
                )
                .build();
    }

    private List<Long> previousValueRows(NumberFilterParams params, Map<String, Object> filterModel) {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("previousValue", filterModel));
        return tradeIds(previousValueQueryBuilder(params).getRows(request));
    }

    @Test
    void includeBlanksInNotEqualParamAddsNulls() {
        List<Long> ids = previousValueRows(
                NumberFilterParams.builder().includeBlanksInNotEqual(true).build(),
                filter("notEqual", 90.0));
        // everything except the exact match (1); nulls (2, 6, 12) included
        assertThat(ids).containsExactly(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
    }

    @Test
    void includeBlanksInLessThanParamAddsNulls() {
        List<Long> ids = previousValueRows(
                NumberFilterParams.builder().includeBlanksInLessThan(true).build(),
                filter("lessThan", 50));
        // 10.5 (4), -5.0 (7) plus nulls (2, 6, 12)
        assertThat(ids).containsExactly(2L, 4L, 6L, 7L, 12L);
    }

    @Test
    void includeBlanksInGreaterThanParamAddsNulls() {
        List<Long> ids = previousValueRows(
                NumberFilterParams.builder().includeBlanksInGreaterThan(true).build(),
                filter("greaterThan", 400));
        // 450.0 (5), 1000.0 (10) plus nulls (2, 6, 12)
        assertThat(ids).containsExactly(2L, 5L, 6L, 10L, 12L);
    }

    @Test
    void includeBlanksInRangeParamAddsNulls() {
        List<Long> ids = previousValueRows(
                NumberFilterParams.builder().includeBlanksInRange(true).build(),
                rangeFilter(60, 100));
        // exclusive range: 90.0 (1), 70.0 (8), 95.5 (11) plus nulls (2, 6, 12)
        assertThat(ids).containsExactly(1L, 2L, 6L, 8L, 11L, 12L);
    }

    @Test
    void combinedConditionsWithOr() {
        LoadSuccessParams result = rows(Map.of(
                "currentValue", combined("OR", filter("lessThan", 0), filter("greaterThan", 500))
        ));
        assertThat(tradeIds(result)).containsExactly(3L, 7L, 10L);
    }
}
