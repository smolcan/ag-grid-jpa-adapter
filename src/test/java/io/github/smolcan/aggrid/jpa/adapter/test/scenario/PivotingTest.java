package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.exceptions.OnPivotMaxColumnsExceededException;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortDirection;
import io.github.smolcan.aggrid.jpa.adapter.request.SortModelItem;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.infrastructure.TestPersistence;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PivotingTest extends ScenarioTestBase {

    private QueryBuilder<Trade, Long, Void> pivotingQueryBuilder(Integer pivotMaxGeneratedColumns) {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).build(),
                        ColDef.builder(Trade_.previousValue).enableValue(true).build(),
                        ColDef.builder(FieldPath.of(Trade_.product).to(Product_.name)).enablePivot(true).build()
                )
                .pivotMaxGeneratedColumns(pivotMaxGeneratedColumns)
                .build();
    }

    private ServerSideGetRowsRequest pivotRequest() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setPivotMode(true);
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getPivotCols().add(groupCol("product.name"));
        request.getValueCols().add(valueCol("currentValue", "sum"));
        request.getSortModel().add(sortItem("portfolio", SortDirection.asc));
        return request;
    }

    @Test
    void generatesPivotResultFieldsFromDistinctValues() {
        LoadSuccessParams result = pivotingQueryBuilder(null).getRows(pivotRequest());
        // one field per distinct product name, joined with the value col by "_"; the order follows
        // where the database sorts nulls, which is not something this test is pinning
        assertThat(result.getPivotResultFields())
                .containsExactlyInAnyOrder("null_currentValue", "Gold_currentValue", "Platinum_currentValue", "Silver_currentValue");
    }

    @Test
    void pivotedAggregationsPerGroup() {
        LoadSuccessParams result = pivotingQueryBuilder(null).getRows(pivotRequest());

        assertThat(columnValues(result, "portfolio"))
                .containsExactly("Alpha", "BETA", "Beta", "Delta", "Epsilon", "Gamma", "alpha", "delta");

        Map<String, Object> alpha = result.getRowData().get(0);
        assertThat(((Number) alpha.get("Gold_currentValue")).doubleValue()).isEqualTo(100.00);
        assertThat(((Number) alpha.get("Silver_currentValue")).doubleValue()).isEqualTo(250.50);
        assertThat(alpha.get("Platinum_currentValue")).isNull();

        Map<String, Object> beta = result.getRowData().get(2);
        assertThat(((Number) beta.get("Platinum_currentValue")).doubleValue()).isEqualTo(0.00);
        assertThat(((Number) beta.get("Silver_currentValue")).doubleValue()).isEqualTo(500.00);
    }

    @Test
    void nullPivotValueAggregatesIntoItsOwnColumn() {
        LoadSuccessParams result = pivotingQueryBuilder(null).getRows(pivotRequest());
        // trade 10 (Delta) has a null product, so it lands in the generated "null_..." column
        Map<String, Object> delta = result.getRowData().get(3);
        assertThat(((Number) delta.get("null_currentValue")).doubleValue()).isEqualTo(999.99);
        assertThat(delta.get("Gold_currentValue")).isNull();
    }

    @Test
    void multipleValueColsMultiplyGeneratedFields() {
        ServerSideGetRowsRequest request = pivotRequest();
        request.getValueCols().add(valueCol("previousValue", "max"));

        LoadSuccessParams result = pivotingQueryBuilder(null).getRows(request);
        assertThat(result.getPivotResultFields()).hasSize(8);
        assertThat(result.getPivotResultFields())
                .contains("Gold_currentValue", "Gold_previousValue", "Silver_previousValue");
    }

    @Test
    void exceedingPivotMaxColumnsThrows() {
        // 1 value col x 3 distinct non-null product names = 3 generated columns > 2 allowed
        assertThatThrownBy(() -> pivotingQueryBuilder(2).getRows(pivotRequest()))
                .isInstanceOf(OnPivotMaxColumnsExceededException.class);
    }

    @Test
    void drillDownAppliesGroupKeyPredicate() {
        ServerSideGetRowsRequest request = pivotRequest();
        request.getGroupKeys().add("Beta");

        LoadSuccessParams result = pivotingQueryBuilder(null).getRows(request);
        assertThat(result.getRowData()).hasSize(1);
        Map<String, Object> beta = result.getRowData().get(0);
        assertThat(beta.get("portfolio")).isEqualTo("Beta");
        assertThat(((Number) beta.get("Silver_currentValue")).doubleValue()).isEqualTo(500.00);
        assertThat(((Number) beta.get("Platinum_currentValue")).doubleValue()).isEqualTo(0.00);
    }

    @Test
    void sortingByPivotedResultColumn() {
        ServerSideGetRowsRequest request = pivotRequest();
        request.getSortModel().clear();
        request.getSortModel().add(sortItem("Gold_currentValue", SortDirection.desc));

        LoadSuccessParams result = pivotingQueryBuilder(null).getRows(request);
        assertThat(result.getRowData()).hasSize(8);
        // the four groups without Gold trades are null here, and descending puts them last where
        // null sorts low and first where it sorts high
        int firstWithGold = TestPersistence.nullsSortLow() ? 0 : 4;
        assertThat(columnValues(result, "portfolio").subList(firstWithGold, firstWithGold + 4))
                .containsExactly("BETA", "delta", "Alpha", "alpha");
    }

    @Test
    void absoluteSortOnPivotResultColumnOrdersByMagnitude() {
        ServerSideGetRowsRequest request = pivotRequest();
        request.getSortModel().clear();
        SortModelItem absoluteSort = sortItem("Platinum_currentValue", SortDirection.desc);
        absoluteSort.setType("absolute");
        request.getSortModel().add(absoluteSort);

        LoadSuccessParams result = pivotingQueryBuilder(null).getRows(request);
        // Platinum sums are Epsilon 100.00, Gamma -10.00, Beta 0.00; without the absolute type
        // Beta (0.00) would outrank Gamma (-10.00). The five groups without Platinum trades are
        // null, and descending puts them last where null sorts low and first where it sorts high
        int firstWithPlatinum = TestPersistence.nullsSortLow() ? 0 : 5;
        assertThat(columnValues(result, "portfolio").subList(firstWithPlatinum, firstWithPlatinum + 3))
                .containsExactly("Epsilon", "Gamma", "Beta");
    }

    @Test
    void absoluteSortOnPivotGroupColumnOrdersGroupsByMagnitude() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.currentValue).enableRowGroup(true, BigDecimal::new).build(),
                        ColDef.builder(Trade_.previousValue).enableValue(true).build(),
                        ColDef.builder(FieldPath.of(Trade_.product).to(Product_.name)).enablePivot(true).build()
                )
                .build();

        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setPivotMode(true);
        request.getRowGroupCols().add(groupCol("currentValue"));
        request.getPivotCols().add(groupCol("product.name"));
        request.getValueCols().add(valueCol("previousValue", "max"));
        SortModelItem absoluteSort = sortItem("currentValue", SortDirection.asc);
        absoluteSort.setType("absolute");
        request.getSortModel().add(absoluteSort);

        LoadSuccessParams result = queryBuilder.getRows(request);
        // magnitude order; signed ascending would instead start with -75.25, -10.00, 0.00.
        // -75.25 and 75.25 tie in the middle, so the assertion brackets them.
        assertThat(doubleValues(result, "currentValue"))
                .hasSize(11)
                .startsWith(0.00, -10.00, 42.42)
                .endsWith(250.50, 320.10, 500.00, 999.99);
    }

    @Test
    void customPivotResultFieldSeparator() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).build(),
                        ColDef.builder(FieldPath.of(Trade_.product).to(Product_.name)).enablePivot(true).build()
                )
                .serverSidePivotResultFieldSeparator("|")
                .build();

        LoadSuccessParams result = queryBuilder.getRows(pivotRequest());
        assertThat(result.getPivotResultFields()).contains("Gold|currentValue", "Silver|currentValue");
    }

    @Test
    void pivotModeWithoutPivotColsBehavesAsGrouping() {
        ServerSideGetRowsRequest request = pivotRequest();
        request.getPivotCols().clear();
        request.setFilterModel(new java.util.HashMap<>());

        LoadSuccessParams result = pivotingQueryBuilder(null).getRows(request);
        assertThat(result.getRowData()).hasSize(8);
        assertThat(result.getPivotResultFields()).isNull();
    }
}
