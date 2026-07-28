package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.SetFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.AgSetColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.DealType;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import io.github.smolcan.aggrid.jpa.adapter.test.infrastructure.TradeTestData;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SetFilterTest extends ScenarioTestBase {

    private QueryBuilder<Trade, Long, Void> setFilterQueryBuilder() {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).filter(AgSetColumnFilter.forString()).build(),
                        ColDef.builder(Trade_.book).filter(AgSetColumnFilter.forString()).build(),
                        ColDef.builder(Trade_.submitterId).filter(AgSetColumnFilter.forNumber()).build(),
                        ColDef.builder(Trade_.tradeDate).filter(AgSetColumnFilter.forDate()).build(),
                        ColDef.builder(Trade_.sold).filter(AgSetColumnFilter.forBoolean()).build(),
                        ColDef.builder(Trade_.dealType).filter(AgSetColumnFilter.forEnum(DealType.class)).build(),
                        ColDef.builder(Trade_.externalId).filter(AgSetColumnFilter.forUUID()).build()
                )
                .build();
    }

    private static Map<String, Object> setFilter(String... values) {
        Map<String, Object> model = new HashMap<>();
        model.put("values", Arrays.asList(values));
        return model;
    }

    private LoadSuccessParams rows(Map<String, Object> filterModel) {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(filterModel);
        return setFilterQueryBuilder().getRows(request);
    }

    @Test
    void matchesSelectedValuesCaseInsensitivelyByDefault() {
        LoadSuccessParams result = rows(Map.of("portfolio", setFilter("alpha", "beta")));
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
    }

    @Test
    void nullOnlySelectionMatchesNullValues() {
        LoadSuccessParams result = rows(Map.of("book", setFilter((String) null)));
        // matches null books (3, 8); the empty-string book (4) is not null
        assertThat(tradeIds(result)).containsExactly(3L, 8L);
    }

    @Test
    void selectionWithValuesAndNull() {
        LoadSuccessParams result = rows(Map.of("book", setFilter("b-1", null)));
        // B-1 (1, 6), b-1 (12) case-insensitively, plus null books (3, 8)
        assertThat(tradeIds(result)).containsExactly(1L, 3L, 6L, 8L, 12L);
    }

    @Test
    void emptySelectionMatchesNothing() {
        LoadSuccessParams result = rows(Map.of("portfolio", setFilter()));
        assertThat(result.getRowData()).isEmpty();
    }

    @Test
    void numberSetFilter() {
        LoadSuccessParams result = rows(Map.of("submitterId", setFilter("101", "112")));
        assertThat(tradeIds(result)).containsExactly(1L, 12L);
    }

    @Test
    void dateSetFilterUsesIsoFormat() {
        LoadSuccessParams result = rows(Map.of("tradeDate", setFilter("2024-01-10", "2025-04-18")));
        assertThat(tradeIds(result)).containsExactly(1L, 12L);
    }

    @Test
    void booleanSetFilter() {
        LoadSuccessParams result = rows(Map.of("sold", setFilter("true")));
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 3L, 9L, 10L);
    }

    @Test
    void booleanSetFilterWithNull() {
        LoadSuccessParams result = rows(Map.of("sold", setFilter("false", null)));
        // false (4, 5, 6, 7, 11) plus null sold (8, 12)
        assertThat(tradeIds(result)).containsExactly(4L, 5L, 6L, 7L, 8L, 11L, 12L);
    }

    @Test
    void enumSetFilter() {
        LoadSuccessParams result = rows(Map.of("dealType", setFilter(DealType.BUY.name())));
        assertThat(tradeIds(result)).containsExactly(1L, 3L, 6L, 9L, 12L);
    }

    @Test
    void uuidSetFilter() {
        LoadSuccessParams result = rows(Map.of("externalId", setFilter(
                TradeTestData.externalId(1).toString(),
                TradeTestData.externalId(12).toString()
        )));
        assertThat(tradeIds(result)).containsExactly(1L, 12L);
    }

    @Test
    void textFormatterParam() {
        // formatter lowercases and strips '-' from both the values and the column
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.book)
                                .filter(AgSetColumnFilter.forString().filterParams(SetFilterParams.builder()
                                        .textFormatter((cb, expr) -> cb.function("REPLACE", String.class, cb.lower(expr), cb.literal("-"), cb.literal("")))
                                        .build()))
                                .build()
                )
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("book", setFilter("b1")));

        // matches B-1 (1, 6) and b-1 (12)
        assertThat(tradeIds(queryBuilder.getRows(request))).containsExactly(1L, 6L, 12L);
    }

    @Test
    void caseSensitiveParam() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio)
                                .filter(AgSetColumnFilter.forString().filterParams(SetFilterParams.builder().caseSensitive(true).build()))
                                .build()
                )
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("portfolio", setFilter("Alpha")));

        assertThat(tradeIds(queryBuilder.getRows(request))).containsExactly(1L, 2L);
    }

    @Test
    void supplySetFilterValuesReturnsSortedDistinctValues() {
        List<String> values = setFilterQueryBuilder().supplySetFilterValues(FieldPath.of(Trade_.portfolio));
        assertThat(values).containsExactly("Alpha", "BETA", "Beta", "Delta", "Epsilon", "Gamma", "alpha", "delta");
    }

    @Test
    void supplySetFilterValuesIncludesNulls() {
        List<Object> values = setFilterQueryBuilder().supplySetFilterValues("book");
        // H2 sorts null first ascending
        assertThat(values).containsExactly(null, "", "B-1", "B-2", "B-3", "B-4", "B-5", "B-6", "b-1");
    }

    @Test
    void supplySetFilterValuesRejectsUnknownOrUnfilteredColumns() {
        assertThatThrownBy(() -> setFilterQueryBuilder().supplySetFilterValues("nope"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> setFilterQueryBuilder().supplySetFilterValues("tradeId"))
                .isInstanceOf(IllegalStateException.class);
    }
}
