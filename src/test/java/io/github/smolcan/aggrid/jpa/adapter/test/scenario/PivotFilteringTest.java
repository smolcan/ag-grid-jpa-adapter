package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Filtering while pivot mode is on: filters restrict the rows the pivot cells are aggregated
 * from, and the same filtering decides which pivot columns are generated.
 */
class PivotFilteringTest extends ScenarioTestBase {

    private QueryBuilder.Builder<Trade, Long, Void> pivotBuilder() {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).filter(new AgNumberColumnFilter<>()).build(),
                        ColDef.builder(FieldPath.of(Trade_.product).to(Product_.name)).enablePivot(true).filter(new AgTextColumnFilter()).build()
                );
    }

    private QueryBuilder<Trade, Long, Void> pivotQueryBuilder() {
        return pivotBuilder().build();
    }

    /** Pivot portfolio x product.name, aggregating sum(currentValue). */
    private ServerSideGetRowsRequest pivotRequest() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setPivotMode(true);
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getPivotCols().add(groupCol("product.name"));
        request.getValueCols().add(valueCol("currentValue", "sum"));
        request.getSortModel().add(sortItem("portfolio", SortDirection.asc));
        return request;
    }

    private static Map<String, Object> filterModel(String field, Map<String, Object> model) {
        Map<String, Object> filterModel = new HashMap<>();
        filterModel.put(field, model);
        return filterModel;
    }

    private static Map<String, Object> row(LoadSuccessParams result, String portfolio) {
        return result.getRowData().stream()
                .filter(row -> portfolio.equals(row.get("portfolio")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no row for portfolio " + portfolio));
    }

    private static Double cell(Map<String, Object> row, String field) {
        Object value = row.get(field);
        return value == null ? null : ((Number) value).doubleValue();
    }

    @Test
    void filterOnPivotColumnShrinksGeneratedColumnsAndRows() {
        ServerSideGetRowsRequest request = pivotRequest();
        request.setFilterModel(filterModel("product.name", filter("equals", "Gold")));

        LoadSuccessParams result = pivotQueryBuilder().getRows(request);
        assertThat(result.getPivotResultFields()).containsExactly("Gold_currentValue");
        // only the portfolios holding gold trades are left
        assertThat(columnValues(result, "portfolio")).containsExactlyInAnyOrder("Alpha", "alpha", "BETA", "delta");
        assertThat(cell(row(result, "Alpha"), "Gold_currentValue")).isEqualTo(100.00);
    }

    @Test
    void filterOnGroupColumnRestrictsRowsAndGeneratedColumns() {
        ServerSideGetRowsRequest request = pivotRequest();
        // text filter is case-insensitive by default, so "Alpha" also matches "alpha"
        request.setFilterModel(filterModel("portfolio", filter("equals", "Alpha")));

        LoadSuccessParams result = pivotQueryBuilder().getRows(request);
        assertThat(columnValues(result, "portfolio")).containsExactlyInAnyOrder("Alpha", "alpha");
        // platinum and the null product are not traded in these portfolios, so no column for them
        assertThat(result.getPivotResultFields())
                .containsExactlyInAnyOrder("Gold_currentValue", "Silver_currentValue");
        assertThat(cell(row(result, "Alpha"), "Silver_currentValue")).isEqualTo(250.50);
        assertThat(cell(row(result, "alpha"), "Gold_currentValue")).isEqualTo(-75.25);
    }

    @Test
    void filterOnValueColumnFiltersRowsBeforeAggregation() {
        ServerSideGetRowsRequest request = pivotRequest();
        request.setFilterModel(filterModel("currentValue", filter("greaterThan", 100)));

        LoadSuccessParams result = pivotQueryBuilder().getRows(request);
        assertThat(columnValues(result, "portfolio"))
                .containsExactlyInAnyOrder("Alpha", "Beta", "BETA", "delta", "Delta");
        assertThat(result.getPivotResultFields())
                .containsExactlyInAnyOrder("Gold_currentValue", "Silver_currentValue", "null_currentValue");
        // the alpha gold trade is 100.00 and does not pass, so only the silver cell is aggregated
        Map<String, Object> alpha = row(result, "Alpha");
        assertThat(cell(alpha, "Silver_currentValue")).isEqualTo(250.50);
        assertThat(cell(alpha, "Gold_currentValue")).isNull();
    }

    @Test
    void quickFilterRestrictsPivotRows() {
        QueryBuilder<Trade, Long, Void> queryBuilder = pivotBuilder()
                .isQuickFilterPresent(true)
                .quickFilterSearchInFields(
                        FieldPath.of(Trade_.portfolio),
                        FieldPath.of(Trade_.product).to(Product_.name)
                )
                .build();

        ServerSideGetRowsRequest request = pivotRequest();
        request.setQuickFilter("gold");

        LoadSuccessParams result = queryBuilder.getRows(request);
        assertThat(result.getPivotResultFields()).containsExactly("Gold_currentValue");
        assertThat(columnValues(result, "portfolio")).containsExactlyInAnyOrder("Alpha", "alpha", "BETA", "delta");
    }

    @Test
    void externalFilterRestrictsPivotRows() {
        QueryBuilder<Trade, Long, Void> queryBuilder = pivotBuilder()
                .isExternalFilterPresent(true)
                .doesExternalFilterPass((cb, root, value) -> cb.equal(root.get(Trade_.portfolio), value))
                .build();

        ServerSideGetRowsRequest request = pivotRequest();
        request.setExternalFilter("Beta");

        LoadSuccessParams result = queryBuilder.getRows(request);
        assertThat(columnValues(result, "portfolio")).containsExactly("Beta");
        assertThat(result.getPivotResultFields())
                .containsExactlyInAnyOrder("Platinum_currentValue", "Silver_currentValue");
        assertThat(cell(row(result, "Beta"), "Silver_currentValue")).isEqualTo(500.00);
    }

    @Test
    void pivotMaxGeneratedColumnsCountsOnlyFilteredRows() {
        ServerSideGetRowsRequest request = pivotRequest();
        request.setFilterModel(filterModel("product.name", filter("equals", "Gold")));

        // unfiltered this generates 3 columns and exceeds the limit (see PivotingTest),
        // filtered down to gold only one column is generated
        QueryBuilder<Trade, Long, Void> queryBuilder = pivotBuilder().pivotMaxGeneratedColumns(2).build();
        assertThatCode(() -> queryBuilder.getRows(request)).doesNotThrowAnyException();
        assertThat(queryBuilder.getRows(request).getPivotResultFields()).containsExactly("Gold_currentValue");
    }

    @Test
    void drillingIntoGroupKeepsTheGeneratedColumns() {
        ServerSideGetRowsRequest request = pivotRequest();
        request.setFilterModel(filterModel("currentValue", filter("greaterThanOrEqual", 100)));
        List<String> fieldsAtRoot = pivotQueryBuilder().getRows(request).getPivotResultFields();

        ServerSideGetRowsRequest drillDown = pivotRequest();
        drillDown.setFilterModel(filterModel("currentValue", filter("greaterThanOrEqual", 100)));
        drillDown.getGroupKeys().add("Alpha");

        LoadSuccessParams result = pivotQueryBuilder().getRows(drillDown);
        // the group key must not narrow the generated columns, only the filters do
        assertThat(result.getPivotResultFields()).containsExactlyInAnyOrderElementsOf(fieldsAtRoot);
        assertThat(result.getPivotResultFields())
                .containsExactlyInAnyOrder("Gold_currentValue", "Silver_currentValue", "Platinum_currentValue", "null_currentValue");
        assertThat(columnValues(result, "portfolio")).containsExactly("Alpha");
        assertThat(cell(row(result, "Alpha"), "Gold_currentValue")).isEqualTo(100.00);
    }

    @Test
    void countRowsCountsTheFilteredGroups() {
        ServerSideGetRowsRequest request = pivotRequest();
        request.setFilterModel(filterModel("product.name", filter("equals", "Gold")));

        assertThat(pivotQueryBuilder().countRows(request)).isEqualTo(4);
    }

    @Test
    void nullFilterModelIsTreatedAsNoFilter() {
        ServerSideGetRowsRequest request = pivotRequest();
        request.setFilterModel(null);

        LoadSuccessParams result = pivotQueryBuilder().getRows(request);
        assertThat(result.getRowData()).hasSize(8);
        assertThat(result.getPivotResultFields()).hasSize(4);
    }

    @Test
    void groupAggFilteringComparesTheFilterAgainstTheAggregatedValue() {
        QueryBuilder<Trade, Long, Void> queryBuilder = pivotBuilder().groupAggFiltering(true).build();

        ServerSideGetRowsRequest request = pivotRequest();
        request.setFilterModel(filterModel("currentValue", filter("greaterThan", 300)));

        LoadSuccessParams result = queryBuilder.getRows(request);
        // group sums: Alpha 350.50, Beta 500.00, BETA 320.10, Delta 999.99 pass;
        // alpha -75.25, Gamma 65.25, delta 150.00, Epsilon 142.42 do not
        assertThat(columnValues(result, "portfolio"))
                .containsExactlyInAnyOrder("Alpha", "Beta", "BETA", "Delta");
        // the cells still aggregate every row of the group, the filter is not applied to them
        Map<String, Object> alpha = row(result, "Alpha");
        assertThat(cell(alpha, "Gold_currentValue")).isEqualTo(100.00);
        assertThat(cell(alpha, "Silver_currentValue")).isEqualTo(250.50);
        // and the generated columns are not narrowed by a filter on an aggregated column
        assertThat(result.getPivotResultFields()).hasSize(4);
    }

    @Test
    void withoutGroupAggFilteringTheSameFilterAppliesToRows() {
        ServerSideGetRowsRequest request = pivotRequest();
        request.setFilterModel(filterModel("currentValue", filter("greaterThan", 300)));

        LoadSuccessParams result = pivotQueryBuilder().getRows(request);
        // only the trades over 300 survive, so Alpha (350.50 in total) is not in the result
        assertThat(columnValues(result, "portfolio")).containsExactlyInAnyOrder("Beta", "BETA", "Delta");
    }

    @Test
    void groupAggFilteringStillRestrictsRowsByTheOtherFilters() {
        QueryBuilder<Trade, Long, Void> queryBuilder = pivotBuilder().groupAggFiltering(true).build();

        ServerSideGetRowsRequest request = pivotRequest();
        request.setFilterModel(filterModel("portfolio", filter("equals", "Gamma")));

        LoadSuccessParams result = queryBuilder.getRows(request);
        assertThat(columnValues(result, "portfolio")).containsExactly("Gamma");
        assertThat(result.getPivotResultFields())
                .containsExactlyInAnyOrder("Platinum_currentValue", "Silver_currentValue");
        assertThat(cell(row(result, "Gamma"), "Platinum_currentValue")).isEqualTo(-10.00);
        assertThat(cell(row(result, "Gamma"), "Silver_currentValue")).isEqualTo(75.25);
    }

    @Test
    void filterOnGeneratedPivotColumnFiltersTheAggregatedCell() {
        ServerSideGetRowsRequest request = pivotRequest();
        // the grid puts a filter on the generated column itself, keyed by its generated name
        request.setFilterModel(filterModel("Gold_currentValue", filter("notBlank")));

        LoadSuccessParams result = pivotQueryBuilder().getRows(request);
        // only the portfolios that traded gold have a value in that cell
        assertThat(columnValues(result, "portfolio")).containsExactlyInAnyOrder("Alpha", "alpha", "BETA", "delta");
        // the filter is on an aggregation, it does not narrow the generated columns
        assertThat(result.getPivotResultFields()).hasSize(4);
        assertThat(cell(row(result, "Alpha"), "Silver_currentValue")).isEqualTo(250.50);
    }

    @Test
    void filterOnGeneratedPivotColumnComparesTheAggregatedValue() {
        ServerSideGetRowsRequest request = pivotRequest();
        request.setFilterModel(filterModel("Silver_currentValue", filter("greaterThan", 300)));

        LoadSuccessParams result = pivotQueryBuilder().getRows(request);
        // silver sums are Alpha 250.50, Beta 500.00, Gamma 75.25, Epsilon 42.42
        assertThat(columnValues(result, "portfolio")).containsExactly("Beta");
        assertThat(cell(row(result, "Beta"), "Silver_currentValue")).isEqualTo(500.00);
    }

    @Test
    void filterOnPivotColumnThatIsNoLongerGeneratedIsIgnored() {
        ServerSideGetRowsRequest request = pivotRequest();
        Map<String, Object> model = filterModel("product.name", filter("equals", "Gold"));
        // the grid can keep a filter on a column that the narrowed pivot no longer generates
        model.put("Silver_currentValue", filter("notBlank"));
        request.setFilterModel(model);

        LoadSuccessParams result = pivotQueryBuilder().getRows(request);
        assertThat(result.getPivotResultFields()).containsExactly("Gold_currentValue");
        assertThat(columnValues(result, "portfolio")).containsExactlyInAnyOrder("Alpha", "alpha", "BETA", "delta");
    }

    @Test
    void filterOnUnknownColumnIsStillReported() {
        ServerSideGetRowsRequest request = pivotRequest();
        request.setFilterModel(filterModel("nonsense", filter("notBlank")));

        assertThatThrownBy(() -> pivotQueryBuilder().getRows(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nonsense");
    }

    @Test
    void countRowsCountsTheGroupsMatchingTheGeneratedPivotColumnFilter() {
        ServerSideGetRowsRequest request = pivotRequest();
        request.setFilterModel(filterModel("Gold_currentValue", filter("notBlank")));

        assertThat(pivotQueryBuilder().countRows(request)).isEqualTo(4);
    }

    @Test
    void suppressAggFilteredOnlyAggregatesOverAllRows() {
        QueryBuilder<Trade, Long, Void> queryBuilder = pivotBuilder().suppressAggFilteredOnly(true).build();

        ServerSideGetRowsRequest request = pivotRequest();
        request.setFilterModel(filterModel("currentValue", filter("greaterThan", 300)));

        LoadSuccessParams result = queryBuilder.getRows(request);
        assertThat(result.getRowData()).hasSize(8);
        assertThat(result.getPivotResultFields()).hasSize(4);
        // aggregations ignore the filter
        assertThat(cell(row(result, "Alpha"), "Gold_currentValue")).isEqualTo(100.00);
    }
}
