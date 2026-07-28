package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.AgSetColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgDateColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgNumberColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdvancedFilterTest extends ScenarioTestBase {

    private QueryBuilder<Trade, Long, Void> advancedFilterQueryBuilder() {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Trade_.book).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Trade_.currentValue).filter(new AgNumberColumnFilter<>()).build(),
                        ColDef.builder(Trade_.previousValue).filter(new AgNumberColumnFilter<>()).build(),
                        ColDef.builder(Trade_.tradeDate).filter(AgDateColumnFilter.forLocalDate()).build(),
                        ColDef.builder(Trade_.sold).filter(AgSetColumnFilter.forBoolean()).build(),
                        ColDef.builder(FieldPath.of(Trade_.product).to(Product_.name)).filter(new AgTextColumnFilter()).build()
                )
                .enableAdvancedFilter(true)
                .build();
    }

    private static Map<String, Object> column(String filterType, String colId, String type, Object filterValue) {
        Map<String, Object> model = new HashMap<>();
        model.put("filterType", filterType);
        model.put("colId", colId);
        model.put("type", type);
        if (filterValue != null) {
            model.put("filter", filterValue);
        }
        return model;
    }

    @SafeVarargs
    private static Map<String, Object> join(String operator, Map<String, Object>... conditions) {
        Map<String, Object> model = new HashMap<>();
        model.put("filterType", "join");
        model.put("type", operator);
        model.put("conditions", List.of(conditions));
        return model;
    }

    private LoadSuccessParams rows(Map<String, Object> filterModel) {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(filterModel);
        return advancedFilterQueryBuilder().getRows(request);
    }

    @Test
    void textColumnFilter() {
        LoadSuccessParams result = rows(column("text", "portfolio", "contains", "alpha"));
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 3L);
    }

    @Test
    void numberColumnFilter() {
        LoadSuccessParams result = rows(column("number", "currentValue", "greaterThan", 320.10));
        assertThat(tradeIds(result)).containsExactly(5L, 10L);
    }

    @Test
    void dateColumnFilterUsesIsoDates() {
        LoadSuccessParams result = rows(column("date", "tradeDate", "lessThan", "2024-04-01"));
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 3L);
    }

    @Test
    void textBlankOnNullAndEmpty() {
        LoadSuccessParams result = rows(column("text", "book", "blank", null));
        assertThat(tradeIds(result)).containsExactly(3L, 4L, 8L);
    }

    @Test
    void nestedPathColumn() {
        LoadSuccessParams result = rows(column("text", "product.name", "endsWith", "old"));
        assertThat(tradeIds(result)).containsExactly(1L, 3L, 6L, 9L);
    }

    @Test
    void joinWithOr() {
        LoadSuccessParams result = rows(join("OR",
                column("text", "portfolio", "equals", "alpha"),
                column("number", "currentValue", "lessThan", 0)
        ));
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 3L, 7L);
    }

    @Test
    void joinWithAnd() {
        LoadSuccessParams result = rows(join("AND",
                column("text", "portfolio", "contains", "a"),
                column("number", "currentValue", "greaterThanOrEqual", 100)
        ));
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 5L, 6L, 9L, 10L);
    }

    @Test
    void nestedJoins() {
        LoadSuccessParams result = rows(join("AND",
                join("OR",
                        column("text", "portfolio", "equals", "alpha"),
                        column("text", "portfolio", "equals", "beta")
                ),
                column("number", "currentValue", "greaterThan", 100)
        ));
        // (alpha or beta) and currentValue > 100 -> 2 (250.50), 5 (500), 6 (320.10)
        assertThat(tradeIds(result)).containsExactly(2L, 5L, 6L);
    }

    @Test
    void textOperatorsNotEqualNotContainsStartsWithNotBlank() {
        assertThat(tradeIds(rows(column("text", "portfolio", "notEqual", "beta"))))
                .containsExactly(1L, 2L, 3L, 7L, 8L, 9L, 10L, 11L, 12L);
        assertThat(tradeIds(rows(column("text", "portfolio", "notContains", "a"))))
                .containsExactly(11L, 12L);
        assertThat(tradeIds(rows(column("text", "portfolio", "startsWith", "del"))))
                .containsExactly(9L, 10L);
        assertThat(tradeIds(rows(column("text", "book", "notBlank", null))))
                .containsExactly(1L, 2L, 5L, 6L, 7L, 9L, 10L, 11L, 12L);
    }

    @Test
    void numberOperatorsEqualsNotEqualLessThan() {
        assertThat(tradeIds(rows(column("number", "currentValue", "equals", 100.00))))
                .containsExactly(1L, 11L);
        assertThat(tradeIds(rows(column("number", "currentValue", "notEqual", 100.00))))
                .containsExactly(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 12L);
        assertThat(tradeIds(rows(column("number", "currentValue", "lessThan", 0))))
                .containsExactly(3L, 7L);
        assertThat(tradeIds(rows(column("number", "currentValue", "lessThanOrEqual", 0))))
                .containsExactly(3L, 4L, 7L);
    }

    @Test
    void numberBlankOperators() {
        assertThat(tradeIds(rows(column("number", "previousValue", "blank", null))))
                .containsExactly(2L, 6L, 12L);
        assertThat(tradeIds(rows(column("number", "previousValue", "notBlank", null))))
                .containsExactly(1L, 3L, 4L, 5L, 7L, 8L, 9L, 10L, 11L);
    }

    @Test
    void dateOperatorsEqualsNotEqualGreaterThanBlank() {
        assertThat(tradeIds(rows(column("date", "tradeDate", "equals", "2024-05-05"))))
                .containsExactly(5L);
        // null date (8) passes neither equals nor notEqual
        assertThat(tradeIds(rows(column("date", "tradeDate", "notEqual", "2024-05-05"))))
                .containsExactly(1L, 2L, 3L, 4L, 6L, 7L, 9L, 10L, 11L, 12L);
        assertThat(tradeIds(rows(column("date", "tradeDate", "greaterThanOrEqual", "2025-01-01"))))
                .containsExactly(9L, 10L, 11L, 12L);
        assertThat(tradeIds(rows(column("date", "tradeDate", "blank", null))))
                .containsExactly(8L);
        assertThat(tradeIds(rows(column("date", "tradeDate", "notBlank", null))))
                .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 9L, 10L, 11L, 12L);
    }

    @Test
    void booleanColumnFilter() {
        assertThat(tradeIds(rows(column("boolean", "sold", "true", null))))
                .containsExactly(1L, 2L, 3L, 9L, 10L);
        assertThat(tradeIds(rows(column("boolean", "sold", "false", null))))
                .containsExactly(4L, 5L, 6L, 7L, 11L);
        assertThat(tradeIds(rows(column("boolean", "sold", "blank", null))))
                .containsExactly(8L, 12L);
        assertThat(tradeIds(rows(column("boolean", "sold", "notBlank", null))))
                .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 9L, 10L, 11L);
    }

    @Test
    void rejectsColumnWithoutFilter() {
        assertThatThrownBy(() -> rows(column("number", "tradeId", "equals", 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsFilterTypeMismatch() {
        assertThatThrownBy(() -> rows(column("text", "currentValue", "contains", "1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-text");
    }
}
