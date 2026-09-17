package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.ColumnAdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.DateFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.NumberFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.SetFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.TextFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.AgSetColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgDateColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgNumberColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.DealType;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import io.github.smolcan.aggrid.jpa.adapter.test.infrastructure.TradeTestData;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
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

    private static Map<String, Object> range(String filterType, String colId, Object from, Object to) {
        Map<String, Object> model = column(filterType, colId, "inRange", from);
        model.put("filterTo", to);
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

    /** Advanced filter over a single extra column whose column filter carries custom params. */
    private List<Long> rowsWithColumn(ColDef<Trade, ?> colDef, Map<String, Object> filterModel) {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(ColDef.builder(Trade_.tradeId).build(), colDef)
                .enableAdvancedFilter(true)
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(filterModel);
        return tradeIds(queryBuilder.getRows(request));
    }

    private static ColDef<Trade, ?> previousValueColumn(NumberFilterParams params) {
        return ColDef.builder(Trade_.previousValue).filter(new AgNumberColumnFilter<Double>().filterParams(params)).build();
    }

    private static ColDef<Trade, ?> tradeDateColumn(DateFilterParams params) {
        return ColDef.builder(Trade_.tradeDate).filter(AgDateColumnFilter.forLocalDate().filterParams(params)).build();
    }

    @Test
    void dateOperatorsLessThanOrEqualAndGreaterThan() {
        assertThat(tradeIds(rows(column("date", "tradeDate", "lessThanOrEqual", "2024-04-01"))))
                .containsExactly(1L, 2L, 3L, 4L);
        // strictly after Jan 1 2025, so that day's trade (9) stays out
        assertThat(tradeIds(rows(column("date", "tradeDate", "greaterThan", "2025-01-01"))))
                .containsExactly(10L, 11L, 12L);
    }

    @Test
    void numberIncludeBlanksParamsOnEqualityOperators() {
        // previousValue is null on 2, 6 and 12
        assertThat(rowsWithColumn(
                previousValueColumn(NumberFilterParams.builder().includeBlanksInEquals(true).build()),
                column("number", "previousValue", "equals", 90)))
                .containsExactly(1L, 2L, 6L, 12L);
        assertThat(rowsWithColumn(
                previousValueColumn(NumberFilterParams.builder().includeBlanksInNotEqual(true).build()),
                column("number", "previousValue", "notEqual", 90)))
                .containsExactly(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
    }

    @Test
    void numberIncludeBlanksParamsOnComparisonOperators() {
        NumberFilterParams lessThanBlanks = NumberFilterParams.builder().includeBlanksInLessThan(true).build();
        assertThat(rowsWithColumn(previousValueColumn(lessThanBlanks), column("number", "previousValue", "lessThan", 60)))
                .containsExactly(2L, 4L, 6L, 7L, 12L);
        assertThat(rowsWithColumn(previousValueColumn(lessThanBlanks), column("number", "previousValue", "lessThanOrEqual", 60)))
                .containsExactly(2L, 3L, 4L, 6L, 7L, 12L);

        NumberFilterParams greaterThanBlanks = NumberFilterParams.builder().includeBlanksInGreaterThan(true).build();
        // nothing is above 1000, so only the blanks come back
        assertThat(rowsWithColumn(previousValueColumn(greaterThanBlanks), column("number", "previousValue", "greaterThan", 1000)))
                .containsExactly(2L, 6L, 12L);
        assertThat(rowsWithColumn(previousValueColumn(greaterThanBlanks), column("number", "previousValue", "greaterThanOrEqual", 1000)))
                .containsExactly(2L, 6L, 10L, 12L);
    }

    @Test
    void dateIncludeBlanksParamsOnEqualityOperators() {
        // tradeDate is null on 8
        assertThat(rowsWithColumn(
                tradeDateColumn(DateFilterParams.builder().includeBlanksInEquals(true).build()),
                column("date", "tradeDate", "equals", "2024-05-05")))
                .containsExactly(5L, 8L);
        assertThat(rowsWithColumn(
                tradeDateColumn(DateFilterParams.builder().includeBlanksInNotEqual(true).build()),
                column("date", "tradeDate", "notEqual", "2024-05-05")))
                .containsExactly(1L, 2L, 3L, 4L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
    }

    @Test
    void dateIncludeBlanksParamsOnComparisonOperators() {
        DateFilterParams lessThanBlanks = DateFilterParams.builder().includeBlanksInLessThan(true).build();
        assertThat(rowsWithColumn(tradeDateColumn(lessThanBlanks), column("date", "tradeDate", "lessThan", "2024-04-01")))
                .containsExactly(1L, 2L, 3L, 8L);
        assertThat(rowsWithColumn(tradeDateColumn(lessThanBlanks), column("date", "tradeDate", "lessThanOrEqual", "2024-04-01")))
                .containsExactly(1L, 2L, 3L, 4L, 8L);

        DateFilterParams greaterThanBlanks = DateFilterParams.builder().includeBlanksInGreaterThan(true).build();
        assertThat(rowsWithColumn(tradeDateColumn(greaterThanBlanks), column("date", "tradeDate", "greaterThan", "2025-01-01")))
                .containsExactly(8L, 10L, 11L, 12L);
        assertThat(rowsWithColumn(tradeDateColumn(greaterThanBlanks), column("date", "tradeDate", "greaterThanOrEqual", "2025-01-01")))
                .containsExactly(8L, 9L, 10L, 11L, 12L);
    }

    @Test
    void numberInRangeIsExclusiveByDefault() {
        // strictly between: only 75.25 (8)
        assertThat(tradeIds(rows(range("number", "currentValue", 42.42, 100.00))))
                .containsExactly(8L);
    }

    @Test
    void numberInRangeParams() {
        // previousValue: 60 (3), 70 (8), 90 (1), 95.5 (11); null on 2, 6, 12
        assertThat(rowsWithColumn(
                previousValueColumn(NumberFilterParams.builder().inRangeInclusive(true).build()),
                range("number", "previousValue", 60, 95.5)))
                .containsExactly(1L, 3L, 8L, 11L);
        assertThat(rowsWithColumn(
                previousValueColumn(NumberFilterParams.builder().includeBlanksInRange(true).build()),
                range("number", "previousValue", 60, 95.5)))
                .containsExactly(1L, 2L, 6L, 8L, 12L);
    }

    @Test
    void dateInRangeIsExclusiveByDefault() {
        // endpoints are trades 2 and 5
        assertThat(tradeIds(rows(range("date", "tradeDate", "2024-02-15", "2024-05-05"))))
                .containsExactly(3L, 4L);
    }

    @Test
    void dateInRangeParams() {
        assertThat(rowsWithColumn(
                tradeDateColumn(DateFilterParams.builder().inRangeInclusive(true).build()),
                range("date", "tradeDate", "2024-02-15", "2024-05-05")))
                .containsExactly(2L, 3L, 4L, 5L);
        // tradeDate is null on 8
        assertThat(rowsWithColumn(
                tradeDateColumn(DateFilterParams.builder().includeBlanksInRange(true).build()),
                range("date", "tradeDate", "2024-02-15", "2024-05-05")))
                .containsExactly(3L, 4L, 8L);
    }

    @Test
    void inRangeInsideJoin() {
        LoadSuccessParams result = rows(join("OR",
                range("number", "currentValue", -100, 0),
                range("dateString", "tradeDate", "2025-02-01", "2025-12-31")
        ));
        // negatives (3, 7) — 0.00 (4) is an excluded endpoint — or traded after Feb 1 2025 (10, 11, 12)
        assertThat(tradeIds(result)).containsExactly(3L, 7L, 10L, 11L, 12L);
    }

    @Test
    void inRangeRequiresFilterTo() {
        assertThatThrownBy(() -> rows(column("number", "currentValue", "inRange", 0)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> rows(column("date", "tradeDate", "inRange", "2024-01-01")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void dateInRangeValidatesFilterTo() {
        assertThatThrownBy(() -> rowsWithColumn(
                tradeDateColumn(DateFilterParams.builder().maxValidDate(LocalDate.of(2024, 12, 31)).build()),
                range("date", "tradeDate", "2024-01-01", "2025-06-01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Max valid date");
    }

    @Test
    void textMatcherOverridesAdvancedTextMatching() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio)
                                .filter(new AgTextColumnFilter().filterParams(TextFilterParams.builder()
                                        // inverting only "contains" proves the requested option reaches the matcher
                                        .textMatcher((cb, params) -> params.getFilterOption() == SimpleFilterModelType.contains
                                                ? cb.notLike(params.getValue(), cb.concat(cb.concat("%", params.getFilterText()), "%"))
                                                : cb.like(params.getValue(), cb.concat(params.getFilterText(), "%")))
                                        .build()))
                                .build()
                )
                .enableAdvancedFilter(true)
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(column("text", "portfolio", "contains", "alpha"));
        // the matcher inverts it, so the alpha trades (1, 2, 3) are exactly the ones dropped
        assertThat(tradeIds(queryBuilder.getRows(request)))
                .containsExactly(4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);

        request.setFilterModel(column("text", "portfolio", "startsWith", "del"));
        assertThat(tradeIds(queryBuilder.getRows(request))).containsExactly(9L, 10L);
    }

    private static Map<String, Object> set(String colId, String type, String... values) {
        Map<String, Object> model = new HashMap<>();
        model.put("filterType", "set");
        model.put("colId", colId);
        model.put("type", type);
        model.put("values", Arrays.asList(values));
        return model;
    }

    private List<Long> setRows(Map<String, Object> filterModel) {
        return setRows(SetFilterParams.builder().build(), filterModel);
    }

    private List<Long> setRows(SetFilterParams portfolioParams, Map<String, Object> filterModel) {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).filter(AgSetColumnFilter.forString().filterParams(portfolioParams)).build(),
                        ColDef.builder(Trade_.submitterId).filter(AgSetColumnFilter.forNumber()).build(),
                        ColDef.builder(Trade_.tradeDate).filter(AgSetColumnFilter.forDate()).build(),
                        ColDef.builder(Trade_.sold).filter(AgSetColumnFilter.forBoolean()).build(),
                        ColDef.builder(Trade_.dealType).filter(AgSetColumnFilter.forEnum(DealType.class)).build(),
                        ColDef.builder(Trade_.externalId).filter(AgSetColumnFilter.forUUID()).build()
                )
                .enableAdvancedFilter(true)
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(filterModel);
        return tradeIds(queryBuilder.getRows(request));
    }

    @Test
    void setMatchesCaseInsensitivelyByDefault() {
        assertThat(setRows(set("portfolio", "isAnyOf", "alpha", "delta")))
                .containsExactly(1L, 2L, 3L, 9L, 10L);
        assertThat(setRows(set("portfolio", "isNoneOf", "alpha", "delta")))
                .containsExactly(4L, 5L, 6L, 7L, 8L, 11L, 12L);
    }

    @Test
    void setCaseSensitiveParam() {
        SetFilterParams caseSensitive = SetFilterParams.builder().caseSensitive(true).build();
        assertThat(setRows(caseSensitive, set("portfolio", "isAnyOf", "alpha")))
                .containsExactly(3L);
        assertThat(setRows(caseSensitive, set("portfolio", "isNoneOf", "alpha")))
                .containsExactly(1L, 2L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
    }

    @Test
    void setIsAnyOfWithBlank() {
        // submitterId is null on 5
        assertThat(setRows(set("submitterId", "isAnyOf", "101", null)))
                .containsExactly(1L, 5L);
        assertThat(setRows(set("submitterId", "isAnyOf", (String) null)))
                .containsExactly(5L);
    }

    @Test
    void setIsNoneOfKeepsBlanksUnlessBlankIsSelected() {
        assertThat(setRows(set("submitterId", "isNoneOf", "101", "102")))
                .containsExactly(3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
        assertThat(setRows(set("submitterId", "isNoneOf", "101", "102", null)))
                .containsExactly(3L, 4L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
        assertThat(setRows(set("submitterId", "isNoneOf", (String) null)))
                .containsExactly(1L, 2L, 3L, 4L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
    }

    @Test
    void setEmptySelection() {
        assertThat(setRows(set("portfolio", "isAnyOf"))).isEmpty();
        assertThat(setRows(set("portfolio", "isNoneOf"))).hasSize(TradeTestData.TRADE_COUNT);
    }

    @Test
    void setParsesValuesWithTheColumnsSetFilter() {
        assertThat(setRows(set("tradeDate", "isAnyOf", "2024-01-10", "2025-04-18")))
                .containsExactly(1L, 12L);
        // sold is null on 8 and 12
        assertThat(setRows(set("sold", "isNoneOf", "true")))
                .containsExactly(4L, 5L, 6L, 7L, 8L, 11L, 12L);
        assertThat(setRows(set("dealType", "isNoneOf", "BUY", "SELL")))
                .containsExactly(4L, 7L, 11L);
        // externalId is null on 10
        assertThat(setRows(set("externalId", "isNoneOf", TradeTestData.externalId(1).toString())))
                .containsExactly(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
    }

    @Test
    void setWorksWithCustomSetFilter() {
        // keys arrive prefixed and compare upper-cased, only this filter knows both
        AgSetColumnFilter<String> customSetFilter = new AgSetColumnFilter<>() {
            @Override
            protected @NotNull Expression<String> modifyColumnExpression(@NotNull CriteriaBuilder cb, @NotNull Expression<String> expression) {
                return cb.upper(expression);
            }

            @Override
            protected @NotNull Expression<String> parseValueToExpression(@NotNull CriteriaBuilder cb, @NotNull String value) {
                return cb.literal(value.substring("p:".length()).toUpperCase());
            }
        };
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).filter(customSetFilter).build()
                )
                .enableAdvancedFilter(true)
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(set("portfolio", "isAnyOf", "p:gamma"));
        assertThat(tradeIds(queryBuilder.getRows(request))).containsExactly(7L, 8L);

        request.setFilterModel(set("portfolio", "isNoneOf", "p:gamma"));
        assertThat(tradeIds(queryBuilder.getRows(request)))
                .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 9L, 10L, 11L, 12L);
    }

    @Test
    void setInsideJoin() {
        LoadSuccessParams result = rows(join("AND",
                set("sold", "isAnyOf", "false"),
                column("number", "currentValue", "greaterThan", 100)
        ));
        // not sold (4, 5, 6, 7, 11) and above 100
        assertThat(tradeIds(result)).containsExactly(5L, 6L);
    }

    @Test
    void rejectsSetFilterOnNonSetColumn() {
        assertThatThrownBy(() -> rows(set("portfolio", "isAnyOf", "alpha")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-set");
    }

    /** currentValue strictly between the condition's filter and filterTo */
    private static ColumnAdvancedFilterModel<Trade, BigDecimal> betweenExclusive(Map<String, Object> filter) {
        BigDecimal from = new BigDecimal(filter.get("filter").toString());
        BigDecimal to = new BigDecimal(filter.get("filterTo").toString());
        return new ColumnAdvancedFilterModel<>("number", FieldPath.of(Trade_.currentValue)) {
            @Override
            public Predicate toPredicate(CriteriaBuilder cb, Root<? extends Trade> root) {
                Expression<BigDecimal> currentValue = getColumnField().getExpression(cb, root);
                return cb.and(cb.gt(currentValue, from), cb.lt(currentValue, to));
            }
        };
    }

    private LoadSuccessParams customOptionRows(Map<String, Object> filterModel) {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Trade_.currentValue).filter(new AgNumberColumnFilter<>()).build()
                )
                .enableAdvancedFilter(true)
                .registerCustomAdvancedFilter("betweenExclusive", AdvancedFilterTest::betweenExclusive)
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(filterModel);
        return queryBuilder.getRows(request);
    }

    @Test
    void customOptionIsFoundByType() {
        // as ag-grid sends it: the column's data type as filterType, the option's displayKey as type
        Map<String, Object> condition = range("number", "currentValue", 30, 300);
        condition.put("type", "betweenExclusive");

        // 42.42 (12), 75.25 (8), 100 (1, 11), 150 (9), 250.50 (2)
        assertThat(tradeIds(customOptionRows(condition)))
                .containsExactly(1L, 2L, 8L, 9L, 11L, 12L);
    }

    @Test
    void customOptionInsideJoin() {
        Map<String, Object> condition = range("number", "currentValue", 30, 300);
        condition.put("type", "betweenExclusive");

        LoadSuccessParams result = customOptionRows(join("AND",
                condition,
                column("text", "portfolio", "contains", "alpha")
        ));
        assertThat(tradeIds(result)).containsExactly(1L, 2L);
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
