package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuickFilterTest extends ScenarioTestBase {

    private QueryBuilder<Trade, Long, Void> quickFilterQueryBuilder(boolean caseSensitive) {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).build(),
                        ColDef.builder(Trade_.book).build(),
                        ColDef.builder(FieldPath.of(Trade_.product).to(Product_.name)).build()
                )
                .isQuickFilterPresent(true)
                .quickFilterSearchInFields(
                        FieldPath.of(Trade_.portfolio),
                        FieldPath.of(Trade_.book),
                        FieldPath.of(Trade_.product).to(Product_.name)
                )
                .quickFilterCaseSensitive(caseSensitive)
                .build();
    }

    private LoadSuccessParams rows(boolean caseSensitive, String quickFilter) {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setQuickFilter(quickFilter);
        return quickFilterQueryBuilder(caseSensitive).getRows(request);
    }

    @Test
    void matchesSingleWordAcrossConfiguredFields() {
        assertThat(tradeIds(rows(false, "alpha"))).containsExactly(1L, 2L, 3L);
    }

    @Test
    void matchesNestedField() {
        assertThat(tradeIds(rows(false, "silver"))).containsExactly(2L, 5L, 8L, 12L);
    }

    @Test
    void allWordsMustMatchTheRow() {
        // "alpha" (portfolio) AND "gold" (product name)
        assertThat(tradeIds(rows(false, "alpha gold"))).containsExactly(1L, 3L);
    }

    @Test
    void wordsMayMatchDifferentFields() {
        // "beta" matches portfolio, "b-1" must match the book of the same row
        assertThat(tradeIds(rows(false, "beta b-1"))).containsExactly(6L);
    }

    @Test
    void noMatchReturnsNoRows() {
        assertThat(rows(false, "zzz").getRowData()).isEmpty();
    }

    @Test
    void nullQuickFilterMatchesEverything() {
        assertThat(tradeIds(rows(false, null))).hasSize(12);
    }

    @Test
    void caseSensitiveMatching() {
        assertThat(tradeIds(rows(true, "Alpha"))).containsExactly(1L, 2L);
    }

    @Test
    void customParserWithTrimInput() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).build(),
                        ColDef.builder(FieldPath.of(Trade_.product).to(Product_.name)).build()
                )
                .isQuickFilterPresent(true)
                .quickFilterSearchInFields(
                        FieldPath.of(Trade_.portfolio),
                        FieldPath.of(Trade_.product).to(Product_.name)
                )
                .quickFilterParser(input -> java.util.Arrays.asList(input.split(",")))
                .quickFilterTrimInput(true)
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setQuickFilter("  alpha  ,  gold  ");

        // comma-separated words, whitespace trimmed by quickFilterTrimInput
        assertThat(tradeIds(queryBuilder.getRows(request))).containsExactly(1L, 3L);
    }

    @Test
    void customMatcherOverridesDefaultLogic() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).build()
                )
                .isQuickFilterPresent(true)
                .quickFilterMatcher((cb, root, words) -> cb.like(root.get(Trade_.portfolio), words.get(0) + "%"))
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setQuickFilter("Del");

        // matcher applies case-sensitive startsWith on the first word only
        assertThat(tradeIds(queryBuilder.getRows(request))).containsExactly(10L);
    }

    @Test
    void textFormatterAppliesToWordsAndFields() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.book).build()
                )
                .isQuickFilterPresent(true)
                .quickFilterSearchInFields(FieldPath.of(Trade_.book))
                .quickFilterTextFormatter((cb, expr) -> cb.function("REPLACE", String.class, expr, cb.literal("-"), cb.literal("")))
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setQuickFilter("b1");

        // formatter strips '-' after lowercasing: matches B-1 (1, 6) and b-1 (12)
        assertThat(tradeIds(queryBuilder.getRows(request))).containsExactly(1L, 6L, 12L);
    }

    @Test
    void builderRequiresSearchFieldsOrMatcher() {
        assertThatThrownBy(() -> QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(ColDef.builder(Trade_.tradeId).build())
                .isQuickFilterPresent(true)
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("quickFilterSearchInFields");
    }
}
