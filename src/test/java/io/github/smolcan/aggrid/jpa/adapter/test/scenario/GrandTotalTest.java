package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GrandTotalTest extends ScenarioTestBase {

    private QueryBuilder<Trade, Long, Void> grandTotalQueryBuilder() {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).build()
                )
                .grandTotalRow(true)
                .build();
    }

    private ServerSideGetRowsRequest grandTotalRequest() {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setNeedsGrandTotal(true);
        request.getValueCols().add(valueCol("currentValue", "sum"));
        return request;
    }

    @Test
    void computesGrandTotalOverAllRows() {
        LoadSuccessParams result = grandTotalQueryBuilder().getRows(grandTotalRequest());
        assertThat(((Number) result.getGrandTotalData().get("currentValue")).doubleValue()).isEqualTo(2453.01);
    }

    @Test
    void grandTotalRespectsFilters() {
        ServerSideGetRowsRequest request = grandTotalRequest();
        request.setFilterModel(Map.of("portfolio", filter("contains", "alpha")));

        LoadSuccessParams result = grandTotalQueryBuilder().getRows(request);
        assertThat(((Number) result.getGrandTotalData().get("currentValue")).doubleValue()).isEqualTo(275.25);
    }

    @Test
    void notComputedWhenNotRequested() {
        ServerSideGetRowsRequest request = grandTotalRequest();
        request.setNeedsGrandTotal(false);

        LoadSuccessParams result = grandTotalQueryBuilder().getRows(request);
        assertThat(result.getGrandTotalData()).isNull();
    }

    @Test
    void emptyValueColsYieldEmptyGrandTotal() {
        ServerSideGetRowsRequest request = grandTotalRequest();
        request.getValueCols().clear();

        LoadSuccessParams result = grandTotalQueryBuilder().getRows(request);
        assertThat(result.getGrandTotalData()).isEmpty();
    }

    @Test
    void grandTotalIgnoresGroupKeysWhenGroupsAreExpanded() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).build()
                )
                .grandTotalRow(true)
                .build();

        ServerSideGetRowsRequest request = grandTotalRequest();
        request.setFilterModel(new java.util.HashMap<>());
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getGroupKeys().add("Beta");

        LoadSuccessParams result = queryBuilder.getRows(request);
        // row data is the drilled-down Beta leaves, but the grand total spans the whole dataset
        assertThat(tradeIds(result)).containsExactly(4L, 5L);
        assertThat(((Number) result.getGrandTotalData().get("currentValue")).doubleValue()).isEqualTo(2453.01);
    }

    @Test
    void directCallRequiresGrandTotalEnabled() {
        assertThatThrownBy(() -> defaultQueryBuilder().getGrandTotalData(grandTotalRequest()))
                .isInstanceOf(IllegalStateException.class);
    }
}
