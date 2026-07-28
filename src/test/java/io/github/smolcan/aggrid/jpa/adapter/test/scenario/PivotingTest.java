package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.exceptions.OnPivotMaxColumnsExceededException;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortDirection;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import org.junit.jupiter.api.Test;

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
        // distinct product names ordered ascending (H2: null first), joined with value col by "_"
        assertThat(result.getPivotResultFields())
                .containsExactly("null_currentValue", "Gold_currentValue", "Platinum_currentValue", "Silver_currentValue");
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
    void nullPivotValueColumnNeverMatches() {
        LoadSuccessParams result = pivotingQueryBuilder(null).getRows(pivotRequest());
        // trade 10 (Delta) has a null product, but SQL "= NULL" never matches,
        // so even the generated "null_..." column stays empty
        Map<String, Object> delta = result.getRowData().get(3);
        assertThat(delta.get("null_currentValue")).isNull();
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
        // groups with Gold values sorted descending; groups without Gold trades (null) sort last on H2
        assertThat(columnValues(result, "portfolio").subList(0, 4))
                .containsExactly("BETA", "delta", "Alpha", "alpha");
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
