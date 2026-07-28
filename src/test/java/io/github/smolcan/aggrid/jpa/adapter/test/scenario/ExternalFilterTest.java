package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalFilterTest extends ScenarioTestBase {

    private QueryBuilder<Trade, Long, Void> externalFilterQueryBuilder() {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Trade_.currentValue).build()
                )
                .isExternalFilterPresent(true)
                .doesExternalFilterPass((cb, root, externalFilterValue) ->
                        cb.gt(root.get(Trade_.currentValue), new BigDecimal(externalFilterValue.toString())))
                .build();
    }

    @Test
    void externalFilterValueFromRequestIsApplied() {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setExternalFilter("300");

        assertThat(tradeIds(externalFilterQueryBuilder().getRows(request))).containsExactly(5L, 6L, 10L);
    }

    @Test
    void externalFilterCombinesWithColumnFilters() {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setExternalFilter("100");
        request.setFilterModel(Map.of("portfolio", filter("contains", "delta")));

        assertThat(tradeIds(externalFilterQueryBuilder().getRows(request))).containsExactly(9L, 10L);
    }

    @Test
    void builderRequiresPredicateFunction() {
        assertThatThrownBy(() -> QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(ColDef.builder(Trade_.tradeId).build())
                .isExternalFilterPresent(true)
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("doesExternalFilterPass");
    }
}
