package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.exceptions.InvalidRequestException;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.AggregationFunction;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestValidationTest extends ScenarioTestBase {

    private QueryBuilder<Trade, Long, Void> validationQueryBuilder() {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).build(),
                        ColDef.builder(Trade_.book).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).build(),
                        ColDef.builder(Trade_.previousValue).enableValue(true).allowedAggFuncs(AggregationFunction.min).build()
                )
                .build();
    }

    private ServerSideGetRowsRequest requestWithEmptyFilterModel() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setFilterModel(new HashMap<>());
        return request;
    }

    @Test
    void rejectsGroupingByUnknownColumn() {
        ServerSideGetRowsRequest request = requestWithEmptyFilterModel();
        request.getRowGroupCols().add(groupCol("nope"));

        assertThatThrownBy(() -> validationQueryBuilder().getRows(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void rejectsGroupingByColumnWithoutRowGroupEnabled() {
        ServerSideGetRowsRequest request = requestWithEmptyFilterModel();
        request.getRowGroupCols().add(groupCol("book"));

        assertThatThrownBy(() -> validationQueryBuilder().getRows(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("grouping enabled");
    }

    @Test
    void rejectsAggregationOnColumnWithoutEnableValue() {
        ServerSideGetRowsRequest request = requestWithEmptyFilterModel();
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getValueCols().add(valueCol("book", "count"));

        assertThatThrownBy(() -> validationQueryBuilder().getRows(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("aggregations disabled");
    }

    @Test
    void rejectsUnknownAggregationFunction() {
        ServerSideGetRowsRequest request = requestWithEmptyFilterModel();
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getValueCols().add(valueCol("currentValue", "median"));

        assertThatThrownBy(() -> validationQueryBuilder().getRows(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("median");
    }

    @Test
    void rejectsAggregationFunctionNotInAllowedList() {
        ServerSideGetRowsRequest request = requestWithEmptyFilterModel();
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getValueCols().add(valueCol("previousValue", "max"));

        assertThatThrownBy(() -> validationQueryBuilder().getRows(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("does not allow");
    }

    @Test
    void rejectsPivotingOnColumnWithoutEnablePivot() {
        ServerSideGetRowsRequest request = requestWithEmptyFilterModel();
        request.setPivotMode(true);
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getPivotCols().add(groupCol("book"));
        request.getValueCols().add(valueCol("currentValue", "sum"));

        assertThatThrownBy(() -> validationQueryBuilder().getRows(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("pivoting enabled");
    }
}
