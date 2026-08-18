package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.column.ComputedField;
import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.AgSetColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgNumberColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortDirection;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ComputedColumnTest extends ScenarioTestBase {

    /** Folds the Alpha/alpha and Beta/BETA case variants together. */
    private static ComputedField<Trade, String> portfolioUpper() {
        return ComputedField.<Trade, String>builder()
                .name("portfolioUpper")
                .javaType(String.class)
                .expressionFunction((cb, root) -> cb.upper(root.get(Trade_.portfolio)))
                .build();
    }

    private static ComputedField<Trade, BigDecimal> valueAfterFee() {
        return ComputedField.<Trade, BigDecimal>builder()
                .name("valueAfterFee")
                .javaType(BigDecimal.class)
                .expressionFunction((cb, root) -> cb.diff(root.get(Trade_.currentValue), new BigDecimal("10.00")))
                .build();
    }

    private static ComputedField<Trade, String> valueBand() {
        return ComputedField.<Trade, String>builder()
                .name("valueBand")
                .javaType(String.class)
                .expressionFunction((cb, root) -> cb.<String>selectCase()
                        .when(cb.greaterThan(root.get(Trade_.currentValue), BigDecimal.ZERO), "POSITIVE")
                        .otherwise("NON_POSITIVE"))
                .build();
    }

    private QueryBuilder.Builder<Trade, Long, Void> config() {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(portfolioUpper())
                                .filter(new AgTextColumnFilter())
                                .enableRowGroup(true, key -> key)
                                .build(),
                        ColDef.builder(valueAfterFee())
                                .filter(new AgNumberColumnFilter<>())
                                .enableValue(true)
                                .build(),
                        ColDef.builder(valueBand())
                                .filter(AgSetColumnFilter.forString())
                                .enableRowGroup(true, key -> key)
                                .build()
                );
    }

    private QueryBuilder<Trade, Long, Void> queryBuilder() {
        return config().build();
    }

    private ServerSideGetRowsRequest groupedBy(String field) {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        // grouping without a filterModel throws inside whereGrouping, so the suite always sends one
        request.setFilterModel(new HashMap<>());
        request.getRowGroupCols().add(groupCol(field));
        request.getValueCols().add(valueCol("valueAfterFee", "sum"));
        request.getSortModel().add(sortItem(field, SortDirection.asc));
        return request;
    }

    @Test
    void computedValueIsSelected() {
        LoadSuccessParams result = queryBuilder().getRows(sortedByIdRequest(0, 4));

        assertThat(columnValues(result, "portfolioUpper")).containsExactly("ALPHA", "ALPHA", "ALPHA", "BETA");
        assertThat(doubleValues(result, "valueAfterFee")).containsExactly(90.00, 240.50, -85.25, -10.00);
    }

    @Test
    void caseExpressionIsSelected() {
        LoadSuccessParams result = queryBuilder().getRows(sortedByIdRequest(0, 4));

        // trade 3 is -75.25 and trade 4 is 0.00, so neither is positive
        assertThat(columnValues(result, "valueBand"))
                .containsExactly("POSITIVE", "POSITIVE", "NON_POSITIVE", "NON_POSITIVE");
    }

    @Test
    void textFilterAppliesToComputedColumn() {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("portfolioUpper", filter("contains", "alpha")));

        assertThat(tradeIds(queryBuilder().getRows(request))).containsExactly(1L, 2L, 3L);
    }

    @Test
    void textFilterMatchesTheComputedValueNotTheUnderlyingColumn() {
        // "ALPHA" only ever exists as a computed value; the stored values are "Alpha" and "alpha"
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("portfolioUpper", filter("equals", "ALPHA")));

        assertThat(tradeIds(queryBuilder().getRows(request))).containsExactly(1L, 2L, 3L);
    }

    @Test
    void numberFilterAppliesToComputedColumn() {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("valueAfterFee", filter("greaterThan", 100)));

        // 240.50, 490.00, 310.10, 140.00, 989.99 clear the fee-adjusted threshold
        assertThat(tradeIds(queryBuilder().getRows(request))).containsExactly(2L, 5L, 6L, 9L, 10L);
    }

    @Test
    void setFilterAppliesToComputedCaseExpression() {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        Map<String, Object> model = new HashMap<>();
        model.put("values", List.of("NON_POSITIVE"));
        request.setFilterModel(Map.of("valueBand", model));

        assertThat(tradeIds(queryBuilder().getRows(request))).containsExactly(3L, 4L, 7L);
    }

    @Test
    void advancedFilterAppliesToComputedColumn() {
        QueryBuilder<Trade, Long, Void> queryBuilder = config().enableAdvancedFilter(true).build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of(
                "filterType", "text", "colId", "portfolioUpper", "type", "contains", "filter", "alpha"));

        assertThat(tradeIds(queryBuilder.getRows(request))).containsExactly(1L, 2L, 3L);
    }

    @Test
    void quickFilterSearchesComputedColumn() {
        QueryBuilder<Trade, Long, Void> queryBuilder = config()
                .isQuickFilterPresent(true)
                .quickFilterSearchInFields(portfolioUpper())
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setQuickFilter("gamma");

        assertThat(tradeIds(queryBuilder.getRows(request))).containsExactly(7L, 8L);
    }

    @Test
    void sortingAppliesToComputedColumn() {
        ServerSideGetRowsRequest request = emptyRequest(0, 3);
        request.getSortModel().add(sortItem("valueAfterFee", SortDirection.desc));

        assertThat(tradeIds(queryBuilder().getRows(request))).containsExactly(10L, 5L, 6L);
    }

    @Test
    void groupingCollapsesRowsByComputedValue() {
        LoadSuccessParams result = queryBuilder().getRows(groupedBy("portfolioUpper"));

        // the five case variants fold into five groups instead of the eight stored spellings
        assertThat(columnValues(result, "portfolioUpper"))
                .containsExactly("ALPHA", "BETA", "DELTA", "EPSILON", "GAMMA");
    }

    @Test
    void aggregatesAreComputedOverTheExpression() {
        LoadSuccessParams result = queryBuilder().getRows(groupedBy("portfolioUpper"));

        // ALPHA is 90.00 + 240.50 - 85.25, BETA is -10.00 + 490.00 + 310.10
        assertThat(doubleValues(result, "valueAfterFee"))
                .containsExactly(245.25, 790.10, 1129.99, 122.42, 45.25);
    }

    @Test
    void drillingIntoAComputedGroupReturnsItsRows() {
        ServerSideGetRowsRequest request = groupedBy("portfolioUpper");
        request.getGroupKeys().add("ALPHA");
        request.getSortModel().clear();
        request.getSortModel().add(sortItem("tradeId", SortDirection.asc));

        assertThat(tradeIds(queryBuilder().getRows(request))).containsExactly(1L, 2L, 3L);
    }

    @Test
    void groupingByACaseExpression() {
        LoadSuccessParams result = queryBuilder().getRows(groupedBy("valueBand"));

        assertThat(columnValues(result, "valueBand")).containsExactly("NON_POSITIVE", "POSITIVE");
        // NON_POSITIVE is trades 3, 4 and 7: -85.25 - 10.00 - 20.00
        assertThat(doubleValues(result, "valueAfterFee")).containsExactly(-115.25, 2448.26);
    }

    @Test
    void countRowsCountsComputedGroups() {
        assertThat(queryBuilder().countRows(groupedBy("portfolioUpper"))).isEqualTo(5);
    }

    @Test
    void pivotingGroupsAndSortsByAComputedColumn() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(valueBand()).enableRowGroup(true, key -> key).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).build(),
                        ColDef.builder(FieldPath.of(Trade_.product).to(Product_.name)).enablePivot(true).build())
                .build();

        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setPivotMode(true);
        request.getRowGroupCols().add(groupCol("valueBand"));
        request.getPivotCols().add(groupCol("product.name"));
        request.getValueCols().add(valueCol("currentValue", "sum"));
        request.getSortModel().add(sortItem("valueBand", SortDirection.desc));

        LoadSuccessParams result = queryBuilder.getRows(request);

        assertThat(columnValues(result, "valueBand")).containsExactly("POSITIVE", "NON_POSITIVE");
        // the non-positive band is trades 3 (-75.25, gold), 4 (0.00, platinum) and 7 (-10.00, platinum)
        Map<String, Object> nonPositive = result.getRowData().get(1);
        assertThat(((Number) nonPositive.get("Gold_currentValue")).doubleValue()).isEqualTo(-75.25);
        assertThat(((Number) nonPositive.get("Platinum_currentValue")).doubleValue()).isEqualTo(-10.00);
    }

    @Test
    void setFilterValuesAreSuppliedForComputedColumn() {
        assertThat(queryBuilder().supplySetFilterValues(portfolioUpper()))
                .containsExactly("ALPHA", "BETA", "DELTA", "EPSILON", "GAMMA");
    }

    @Test
    void navigatingAnAssociationInsideAComputedExpressionInnerJoins() {
        ComputedField<Trade, String> viaComputed = ComputedField.<Trade, String>builder()
                .name("productUpper")
                .javaType(String.class)
                .expressionFunction((cb, root) -> cb.upper(root.get(Trade_.product).get(Product_.name)))
                .build();

        QueryBuilder<Trade, Long, Void> computed = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(ColDef.builder(Trade_.tradeId).build(), ColDef.builder(viaComputed).build())
                .build();
        QueryBuilder<Trade, Long, Void> mapped = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(FieldPath.of(Trade_.product).to(Product_.name)).build())
                .build();

        assertThat(tradeIds(computed.getRows(sortedByIdRequest(0, 100))))
                .doesNotContain(10L)
                .hasSize(11);
        assertThat(tradeIds(mapped.getRows(sortedByIdRequest(0, 100)))).contains(10L).hasSize(12);
    }
}
