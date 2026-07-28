package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.MultiFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.AgMultiColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.AgSetColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiFilterTest extends ScenarioTestBase {

    private QueryBuilder<Trade, Long, Void> multiFilterQueryBuilder() {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio)
                                .filter(new AgMultiColumnFilter<String>().filterParams(MultiFilterParams.<String>builder()
                                        .filters(new AgTextColumnFilter(), AgSetColumnFilter.forString())
                                        .build()))
                                .build()
                )
                .build();
    }

    private static Map<String, Object> multiFilter(List<Map<String, Object>> filterModels) {
        Map<String, Object> model = new HashMap<>();
        model.put("filterModels", filterModels);
        return model;
    }

    private static Map<String, Object> setFilter(String... values) {
        Map<String, Object> model = new HashMap<>();
        model.put("values", Arrays.asList(values));
        return model;
    }

    private LoadSuccessParams rows(Map<String, Object> filterModel) {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(filterModel);
        return multiFilterQueryBuilder().getRows(request);
    }

    @Test
    void combinesInnerFiltersWithAnd() {
        LoadSuccessParams result = rows(Map.of("portfolio", multiFilter(Arrays.asList(
                filter("contains", "a"),
                setFilter("alpha", "beta", "gamma")
        ))));
        // contains 'a' keeps 1-10, set keeps alpha/beta/gamma portfolios (1-8)
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
    }

    @Test
    void nullEntriesAreInactive() {
        LoadSuccessParams result = rows(Map.of("portfolio", multiFilter(Arrays.asList(
                null,
                setFilter("delta")
        ))));
        assertThat(tradeIds(result)).containsExactly(9L, 10L);
    }

    @Test
    void filterModelCountMustMatchConfiguredFilters() {
        assertThatThrownBy(() -> rows(Map.of("portfolio", multiFilter(List.of(filter("contains", "a"))))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("filterModels.size()");
    }
}
