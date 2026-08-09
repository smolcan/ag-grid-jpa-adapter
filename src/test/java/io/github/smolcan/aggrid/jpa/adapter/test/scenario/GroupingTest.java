package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgNumberColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortDirection;
import io.github.smolcan.aggrid.jpa.adapter.request.SortModelItem;
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
import static org.assertj.core.api.Assertions.within;

class GroupingTest extends ScenarioTestBase {

    private QueryBuilder<Trade, Long, Void> groupingQueryBuilder() {
        return groupingQueryBuilder(false);
    }

    private QueryBuilder<Trade, Long, Void> groupingQueryBuilder(boolean withChildCount) {
        QueryBuilder.Builder<Trade, Long, Void> builder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).enableValue(true).build(),
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Trade_.book).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).enableRowGroup(true, BigDecimal::new).filter(new AgNumberColumnFilter<>()).build(),
                        ColDef.builder(Trade_.previousValue).enableValue(true).build(),
                        ColDef.builder(FieldPath.of(Trade_.product).to(Product_.name)).enableRowGroup(true, key -> key).build()
                );
        if (withChildCount) {
            builder.getChildCount(true).getChildCountFieldName("childCount");
        }
        return builder.build();
    }

    /**
     * Grouping request sorted by the group column; AG Grid always sends a filterModel
     * object (possibly empty), and grouping mode relies on it being non-null.
     */
    private ServerSideGetRowsRequest groupedByPortfolioRequest(String... groupKeys) {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setFilterModel(new HashMap<>());
        request.getRowGroupCols().add(groupCol("portfolio"));
        for (String key : groupKeys) {
            request.getGroupKeys().add(key);
        }
        request.getSortModel().add(sortItem("portfolio", SortDirection.asc));
        return request;
    }

    @Test
    void topLevelReturnsDistinctGroups() {
        LoadSuccessParams result = groupingQueryBuilder().getRows(groupedByPortfolioRequest());
        // H2 binary collation: uppercase sorts before lowercase
        assertThat(columnValues(result, "portfolio"))
                .containsExactly("Alpha", "BETA", "Beta", "Delta", "Epsilon", "Gamma", "alpha", "delta");
    }

    @Test
    void groupRowsContainOnlyGroupAndAggregationColumns() {
        ServerSideGetRowsRequest request = groupedByPortfolioRequest();
        request.getValueCols().add(valueCol("currentValue", "sum"));

        LoadSuccessParams result = groupingQueryBuilder().getRows(request);
        Map<String, Object> firstRow = result.getRowData().get(0);
        assertThat(firstRow).containsKeys("portfolio", "currentValue");
        assertThat(firstRow.get("tradeId")).isNull();
    }

    @Test
    void sumAggregationPerGroup() {
        ServerSideGetRowsRequest request = groupedByPortfolioRequest();
        request.getValueCols().add(valueCol("currentValue", "sum"));

        LoadSuccessParams result = groupingQueryBuilder().getRows(request);
        assertThat(doubleValues(result, "currentValue"))
                .containsExactly(350.50, 320.10, 500.00, 999.99, 142.42, 65.25, -75.25, 150.00);
    }

    @Test
    void minMaxAndCountAggregations() {
        ServerSideGetRowsRequest request = groupedByPortfolioRequest();
        request.getValueCols().add(valueCol("currentValue", "min"));
        request.getValueCols().add(valueCol("previousValue", "max"));
        request.getValueCols().add(valueCol("tradeId", "count"));

        LoadSuccessParams result = groupingQueryBuilder().getRows(request);
        assertThat(doubleValues(result, "currentValue"))
                .containsExactly(100.00, 320.10, 0.00, 999.99, 42.42, -10.00, -75.25, 150.00);
        // BETA has only a null previousValue -> null aggregate
        assertThat(doubleValues(result, "previousValue"))
                .containsExactly(90.0, null, 450.0, 1000.0, 95.5, 70.0, 60.0, 140.0);
        assertThat(doubleValues(result, "tradeId"))
                .containsExactly(2.0, 1.0, 2.0, 1.0, 2.0, 2.0, 1.0, 1.0);
    }

    @Test
    void childCountPerGroup() {
        ServerSideGetRowsRequest request = groupedByPortfolioRequest();

        LoadSuccessParams result = groupingQueryBuilder(true).getRows(request);
        assertThat(doubleValues(result, "childCount"))
                .containsExactly(2.0, 1.0, 2.0, 1.0, 2.0, 2.0, 1.0, 1.0);
    }

    @Test
    void drillDownReturnsLeafRowsOfExactGroup() {
        ServerSideGetRowsRequest request = groupedByPortfolioRequest("Beta");
        request.getSortModel().add(sortItem("tradeId", SortDirection.asc));

        LoadSuccessParams result = groupingQueryBuilder().getRows(request);
        // group key match is exact: "Beta" does not include "BETA"
        assertThat(tradeIds(result)).containsExactly(4L, 5L);
    }

    @Test
    void multiLevelGroupingSecondLevel() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setFilterModel(new HashMap<>());
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getRowGroupCols().add(groupCol("product.name"));
        request.getGroupKeys().add("Alpha");
        request.getSortModel().add(sortItem("product.name", SortDirection.asc));
        request.getValueCols().add(valueCol("currentValue", "sum"));

        LoadSuccessParams result = groupingQueryBuilder().getRows(request);
        assertThat(result.getRowData()).hasSize(2);
        assertThat(nestedValue(result.getRowData().get(0), "product.name")).isEqualTo("Gold");
        assertThat(nestedValue(result.getRowData().get(1), "product.name")).isEqualTo("Silver");
        assertThat(doubleValues(result, "currentValue")).containsExactly(100.00, 250.50);
    }

    @Test
    void multiLevelGroupingLeafLevel() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setFilterModel(new HashMap<>());
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getRowGroupCols().add(groupCol("product.name"));
        request.getGroupKeys().add("Alpha");
        request.getGroupKeys().add("Silver");

        LoadSuccessParams result = groupingQueryBuilder().getRows(request);
        assertThat(tradeIds(result)).containsExactly(2L);
    }

    @Test
    void filterAppliesToLeavesBeforeAggregation() {
        ServerSideGetRowsRequest request = groupedByPortfolioRequest();
        request.getValueCols().add(valueCol("currentValue", "sum"));
        request.setFilterModel(Map.of("currentValue", filter("greaterThan", 0)));

        LoadSuccessParams result = groupingQueryBuilder().getRows(request);
        // trades with currentValue <= 0 (3, 4, 7) are excluded before grouping; group "alpha" disappears
        assertThat(columnValues(result, "portfolio"))
                .containsExactly("Alpha", "BETA", "Beta", "Delta", "Epsilon", "Gamma", "delta");
        assertThat(doubleValues(result, "currentValue"))
                .containsExactly(350.50, 320.10, 500.00, 999.99, 142.42, 75.25, 150.00);
    }

    @Test
    void sortingByAggregatedColumn() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setFilterModel(new HashMap<>());
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getValueCols().add(valueCol("currentValue", "sum"));
        request.getSortModel().add(sortItem("currentValue", SortDirection.desc));

        LoadSuccessParams result = groupingQueryBuilder().getRows(request);
        assertThat(columnValues(result, "portfolio"))
                .containsExactly("Delta", "Beta", "Alpha", "BETA", "delta", "Epsilon", "Gamma", "alpha");
    }

    @Test
    void absoluteSortOnGroupColumnOrdersGroupsByMagnitude() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        // -75.25 would tie with 75.25 in absolute value, so keep it out of the comparison
        request.setFilterModel(Map.of("currentValue", filter("greaterThan", -50)));
        request.getRowGroupCols().add(groupCol("currentValue"));
        SortModelItem absoluteSort = sortItem("currentValue", SortDirection.asc);
        absoluteSort.setType("absolute");
        request.getSortModel().add(absoluteSort);

        LoadSuccessParams result = groupingQueryBuilder().getRows(request);
        // the -10.00 group sorts between 0.00 and 42.42 by magnitude
        assertThat(doubleValues(result, "currentValue"))
                .containsExactly(0.00, -10.00, 42.42, 75.25, 100.00, 150.00, 250.50, 320.10, 500.00, 999.99);
    }

    @Test
    void absoluteSortOnAggregatedColumnOrdersGroupsByMagnitude() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setFilterModel(new HashMap<>());
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getValueCols().add(valueCol("currentValue", "sum"));
        SortModelItem absoluteSort = sortItem("currentValue", SortDirection.asc);
        absoluteSort.setType("absolute");
        request.getSortModel().add(absoluteSort);

        LoadSuccessParams result = groupingQueryBuilder().getRows(request);
        // "alpha" sums to -75.25, so magnitude puts it second instead of first (compare sortingByAggregatedColumn)
        assertThat(columnValues(result, "portfolio"))
                .containsExactly("Gamma", "alpha", "Epsilon", "delta", "BETA", "Alpha", "Beta", "Delta");
    }

    @Test
    void groupPaginationAppliesToGroups() {
        ServerSideGetRowsRequest request = groupedByPortfolioRequest();
        request.setEndRow(3);

        LoadSuccessParams result = groupingQueryBuilder().getRows(request);
        assertThat(columnValues(result, "portfolio")).containsExactly("Alpha", "BETA", "Beta");
    }

    @Test
    void countRowsCountsGroupsAtRootLevel() {
        long count = groupingQueryBuilder().countRows(groupedByPortfolioRequest());
        assertThat(count).isEqualTo(8);
    }

    @Test
    void countRowsAppliesFilterToCountedGroups() {
        ServerSideGetRowsRequest request = groupedByPortfolioRequest();
        request.getValueCols().add(valueCol("currentValue", "sum"));
        request.setFilterModel(Map.of("currentValue", filter("greaterThan", 0)));

        // the filter runs inside the counting subquery: "alpha" keeps no leaf above 0 and drops out,
        // leaving the same 7 groups as filterAppliesToLeavesBeforeAggregation
        assertThat(groupingQueryBuilder().countRows(request)).isEqualTo(7);
    }

    @Test
    void avgAggregationPerGroup() {
        ServerSideGetRowsRequest request = groupedByPortfolioRequest();
        request.getValueCols().add(valueCol("currentValue", "avg"));

        LoadSuccessParams result = groupingQueryBuilder().getRows(request);
        List<Double> averages = doubleValues(result, "currentValue");
        List<Double> expected = List.of(175.25, 320.10, 250.00, 999.99, 71.21, 32.625, -75.25, 150.00);
        assertThat(averages).hasSize(8);
        for (int i = 0; i < expected.size(); i++) {
            assertThat(averages.get(i)).isCloseTo(expected.get(i), within(0.0001));
        }
    }

    @Test
    void groupingByNumericColumnUsesKeyConverter() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setFilterModel(new HashMap<>());
        request.getRowGroupCols().add(groupCol("currentValue"));
        request.getGroupKeys().add("100.00");
        request.getSortModel().add(sortItem("tradeId", SortDirection.asc));

        LoadSuccessParams result = groupingQueryBuilder().getRows(request);
        assertThat(tradeIds(result)).containsExactly(1L, 11L);
    }

    @Test
    void suppressAggFilteredOnlyIgnoresFiltersOnGroupLevel() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).filter(new AgNumberColumnFilter<>()).build()
                )
                .suppressAggFilteredOnly(true)
                .build();

        ServerSideGetRowsRequest request = groupedByPortfolioRequest();
        request.getValueCols().add(valueCol("currentValue", "sum"));
        request.setFilterModel(Map.of("currentValue", filter("greaterThan", 0)));

        LoadSuccessParams result = queryBuilder.getRows(request);
        // filter is ignored above leaf level: all 8 groups with unfiltered sums (compare filterAppliesToLeavesBeforeAggregation)
        assertThat(columnValues(result, "portfolio"))
                .containsExactly("Alpha", "BETA", "Beta", "Delta", "Epsilon", "Gamma", "alpha", "delta");
        assertThat(doubleValues(result, "currentValue"))
                .containsExactly(350.50, 320.10, 500.00, 999.99, 142.42, 65.25, -75.25, 150.00);

        // ...but still applies once drilled down to leaf rows
        ServerSideGetRowsRequest leafRequest = groupedByPortfolioRequest("Gamma");
        leafRequest.setFilterModel(Map.of("currentValue", filter("greaterThan", 0)));
        leafRequest.getSortModel().add(sortItem("tradeId", SortDirection.asc));
        assertThat(tradeIds(queryBuilder.getRows(leafRequest))).containsExactly(8L);
    }

    @Test
    void groupAggFilteringMatchesFilterAgainstGroupAggregates() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).filter(new AgNumberColumnFilter<>()).build()
                )
                .groupAggFiltering(true)
                .build();

        ServerSideGetRowsRequest request = groupedByPortfolioRequest();
        request.getValueCols().add(valueCol("currentValue", "sum"));
        request.setFilterModel(Map.of("currentValue", filter("greaterThan", 300)));

        LoadSuccessParams result = queryBuilder.getRows(request);
        // groups whose SUM exceeds 300 (or containing a matching leaf); matching groups keep all their leaves
        assertThat(columnValues(result, "portfolio")).containsExactly("Alpha", "BETA", "Beta", "Delta");
        assertThat(doubleValues(result, "currentValue")).containsExactly(350.50, 320.10, 500.00, 999.99);
    }

    private QueryBuilder<Trade, Long, Void> groupAggFilteringQueryBuilder() {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(FieldPath.of(Trade_.product).to(Product_.name)).enableRowGroup(true, key -> key).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).filter(new AgNumberColumnFilter<>()).build()
                )
                .groupAggFiltering(true)
                .build();
    }

    @Test
    void groupAggFilteringMatchingExpandedParentExposesAllChildGroups() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getRowGroupCols().add(groupCol("product.name"));
        request.getGroupKeys().add("Alpha");
        request.getValueCols().add(valueCol("currentValue", "sum"));
        request.setFilterModel(Map.of("currentValue", filter("greaterThan", 300)));
        request.getSortModel().add(sortItem("product.name", SortDirection.asc));

        LoadSuccessParams result = groupAggFilteringQueryBuilder().getRows(request);
        // the expanded parent Alpha (sum 350.50) passes the aggregate filter, so both
        // child groups appear even though neither passes on its own
        assertThat(result.getRowData()).hasSize(2);
        assertThat(nestedValue(result.getRowData().get(0), "product.name")).isEqualTo("Gold");
        assertThat(nestedValue(result.getRowData().get(1), "product.name")).isEqualTo("Silver");
        assertThat(doubleValues(result, "currentValue")).containsExactly(100.00, 250.50);
    }

    @Test
    void groupAggFilteringNonMatchingExpandedParentFiltersChildGroups() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getRowGroupCols().add(groupCol("product.name"));
        request.getGroupKeys().add("Gamma");
        request.getValueCols().add(valueCol("currentValue", "sum"));
        request.setFilterModel(Map.of("currentValue", filter("greaterThan", 300)));

        LoadSuccessParams result = groupAggFilteringQueryBuilder().getRows(request);
        // Gamma (sum 65.25) fails the filter and no child group or leaf passes either
        assertThat(result.getRowData()).isEmpty();
    }

    @Test
    void groupAggFilteringAppliesNonAggregatedFilterAtLeafLevel() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getGroupKeys().add("Beta");
        request.setFilterModel(Map.of("portfolio", filter("contains", "beta")));
        request.getSortModel().add(sortItem("tradeId", SortDirection.asc));

        LoadSuccessParams result = groupAggFilteringQueryBuilder().getRows(request);
        assertThat(tradeIds(result)).containsExactly(4L, 5L);
    }

    @Test
    void paginateChildRowsCountsRowsInsideExpandedGroups() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).build()
                )
                .paginateChildRows(true)
                .build();

        // no expanded groups: still counts root groups
        assertThat(queryBuilder.countRows(groupedByPortfolioRequest())).isEqualTo(8);
        // group expanded: counts the rows inside it
        assertThat(queryBuilder.countRows(groupedByPortfolioRequest("Beta"))).isEqualTo(2);
    }

    @Test
    void paginateChildRowsCountsChildGroupsOfExpandedGroup() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).build(),
                        ColDef.builder(FieldPath.of(Trade_.product).to(Product_.name)).enableRowGroup(true, key -> key).build()
                )
                .paginateChildRows(true)
                .build();

        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setFilterModel(new HashMap<>());
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getRowGroupCols().add(groupCol("product.name"));
        request.getGroupKeys().add("Alpha");

        // a level is still collapsed, so groups are counted rather than rows, and the expanded
        // group key narrows the count to Alpha's child groups (Gold, Silver)
        assertThat(queryBuilder.countRows(request)).isEqualTo(2);
    }

    @Test
    void customAggregationFunction() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).build()
                )
                // STDDEV_POP is spelled the same in H2, Postgres and MariaDB, so the registered
                // function reaches the database unchanged whatever the provider and dialect
                .registerCustomAggFunction("stdDev", (cb, expr) -> cb.function("STDDEV_POP", Double.class, expr))
                .build();

        ServerSideGetRowsRequest request = groupedByPortfolioRequest();
        request.getValueCols().add(valueCol("currentValue", "stdDev"));

        LoadSuccessParams result = queryBuilder.getRows(request);
        // half the spread for the two-trade groups (Alpha: (250.50 - 100.00) / 2), zero for the
        // single-trade ones; population deviation rather than sample so those stay 0 instead of null
        List<Double> deviations = doubleValues(result, "currentValue");
        List<Double> expected = List.of(75.25, 0.0, 250.00, 0.0, 28.79, 42.625, 0.0, 0.0);
        assertThat(deviations).hasSize(8);
        for (int i = 0; i < expected.size(); i++) {
            assertThat(deviations.get(i)).isCloseTo(expected.get(i), within(0.0001));
        }
    }
}
