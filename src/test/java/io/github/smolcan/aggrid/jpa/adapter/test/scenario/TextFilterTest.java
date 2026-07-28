package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.TextFilterParams;
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

class TextFilterTest extends ScenarioTestBase {

    private LoadSuccessParams rows(Map<String, Object> filterModel) {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(filterModel);
        return defaultQueryBuilder().getRows(request);
    }

    @Test
    void containsIsCaseInsensitiveByDefault() {
        LoadSuccessParams result = rows(Map.of("portfolio", filter("contains", "alpha")));
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 3L);
    }

    @Test
    void notContainsExcludesMatches() {
        LoadSuccessParams result = rows(Map.of("portfolio", filter("notContains", "a")));
        // only Epsilon has no 'a'
        assertThat(tradeIds(result)).containsExactly(11L, 12L);
    }

    @Test
    void equalsMatchesWholeValue() {
        LoadSuccessParams result = rows(Map.of("portfolio", filter("equals", "beta")));
        assertThat(tradeIds(result)).containsExactly(4L, 5L, 6L);
    }

    @Test
    void notEqualExcludesWholeValue() {
        LoadSuccessParams result = rows(Map.of("portfolio", filter("notEqual", "beta")));
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 3L, 7L, 8L, 9L, 10L, 11L, 12L);
    }

    @Test
    void startsWith() {
        LoadSuccessParams result = rows(Map.of("portfolio", filter("startsWith", "del")));
        assertThat(tradeIds(result)).containsExactly(9L, 10L);
    }

    @Test
    void endsWith() {
        LoadSuccessParams result = rows(Map.of("portfolio", filter("endsWith", "ta")));
        assertThat(tradeIds(result)).containsExactly(4L, 5L, 6L, 9L, 10L);
    }

    @Test
    void blankMatchesNullAndEmptyString() {
        LoadSuccessParams result = rows(Map.of("book", filter("blank")));
        assertThat(tradeIds(result)).containsExactly(3L, 4L, 8L);
    }

    @Test
    void notBlankExcludesNullAndEmptyString() {
        LoadSuccessParams result = rows(Map.of("book", filter("notBlank")));
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 5L, 6L, 7L, 9L, 10L, 11L, 12L);
    }

    @Test
    void combinedConditionsWithOr() {
        LoadSuccessParams result = rows(Map.of(
                "portfolio", combined("OR", filter("equals", "alpha"), filter("equals", "gamma"))
        ));
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 3L, 7L, 8L);
    }

    @Test
    void combinedConditionsWithAnd() {
        LoadSuccessParams result = rows(Map.of(
                "portfolio", combined("AND", filter("contains", "a"), filter("notContains", "et"))
        ));
        // contains 'a' removes Epsilon; notContains 'et' removes Beta/BETA
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 3L, 7L, 8L, 9L, 10L);
    }

    @Test
    void filtersAcrossMultipleColumnsAreAnded() {
        LoadSuccessParams result = rows(Map.of(
                "portfolio", filter("contains", "a"),
                "book", filter("equals", "b-1")
        ));
        // portfolio with 'a' (1-10) AND book B-1/b-1 (1, 6, 12) -> 1, 6
        assertThat(tradeIds(result)).containsExactly(1L, 6L);
    }

    @Test
    void caseSensitiveParamRespectsCase() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio)
                                .filter(new AgTextColumnFilter().filterParams(TextFilterParams.builder().caseSensitive(true).build()))
                                .build()
                )
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("portfolio", filter("equals", "Alpha")));

        LoadSuccessParams result = queryBuilder.getRows(request);
        // 'alpha' (3) no longer matches
        assertThat(tradeIds(result)).containsExactly(1L, 2L);
    }

    @Test
    void trimInputParamTrimsFilterValue() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio)
                                .filter(new AgTextColumnFilter().filterParams(TextFilterParams.builder().trimInput(true).build()))
                                .build()
                )
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("portfolio", filter("equals", "  beta  ")));

        LoadSuccessParams result = queryBuilder.getRows(request);
        assertThat(tradeIds(result)).containsExactly(4L, 5L, 6L);
    }

    @Test
    void filtersOnNestedPath() {
        LoadSuccessParams result = rows(Map.of("product.name", filter("contains", "old")));
        // Gold trades; null product (10) is excluded, not an error
        assertThat(tradeIds(result)).containsExactly(1L, 3L, 6L, 9L);
    }

    private QueryBuilder<Trade, Long, Void> bookFilterQueryBuilder(TextFilterParams params) {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.book).filter(new AgTextColumnFilter().filterParams(params)).build()
                )
                .build();
    }

    @Test
    void filterOptionsRestrictAllowedOperations() {
        QueryBuilder<Trade, Long, Void> queryBuilder = bookFilterQueryBuilder(
                TextFilterParams.builder().filterOptions(SimpleFilterModelType.contains).build());

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("book", filter("equals", "B-1")));

        assertThatThrownBy(() -> queryBuilder.getRows(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void textFormatterReplacesDefaultCaseHandling() {
        // formatter lowercases and strips '-' from both the value and the filter input
        QueryBuilder<Trade, Long, Void> queryBuilder = bookFilterQueryBuilder(
                TextFilterParams.builder()
                        .textFormatter((cb, expr) -> cb.function("REPLACE", String.class, cb.lower(expr), cb.literal("-"), cb.literal("")))
                        .build());

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("book", filter("equals", "b1")));

        // matches B-1 (1, 6) and b-1 (12)
        assertThat(tradeIds(queryBuilder.getRows(request))).containsExactly(1L, 6L, 12L);
    }

    @Test
    void textMatcherOverridesMatchingLogic() {
        // matcher applies startsWith semantics regardless of the requested filter type
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio)
                                .filter(new AgTextColumnFilter().filterParams(TextFilterParams.builder()
                                        .textMatcher((cb, params) -> cb.like(params.getValue(), cb.concat(params.getFilterText(), "%")))
                                        .build()))
                                .build()
                )
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("portfolio", filter("contains", "Del")));

        // default case-insensitivity still lowercases both sides before the matcher runs
        assertThat(tradeIds(queryBuilder.getRows(request))).containsExactly(9L, 10L);
    }

    @Test
    void rejectsFilterOnColumnWithoutFilter() {
        assertThatThrownBy(() -> rows(Map.of("tradeId", filter("equals", "1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not filterable");
    }

    @Test
    void rejectsFilterOnUnknownColumn() {
        assertThatThrownBy(() -> rows(Map.of("nope", filter("equals", "x"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nope");
    }
}
