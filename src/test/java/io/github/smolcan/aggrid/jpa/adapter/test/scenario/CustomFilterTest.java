package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.filter.IFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.IFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.IFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Custom filter implementation following the docs' even/odd example
 * (custom IFilterModel + IFilterParams + IFilter subclass).
 */
class CustomFilterTest extends ScenarioTestBase {

    @Setter
    @Getter
    static class CustomNumberFilterModel implements IFilterModel {
        private String value;

    }

    @Getter
    static class CustomNumberFilterParams implements IFilterParams {
        private final boolean includeNullValues;

        CustomNumberFilterParams(boolean includeNullValues) {
            this.includeNullValues = includeNullValues;
        }

    }

    static class CustomNumberFilter<T extends Number> extends IFilter<T, CustomNumberFilterModel, CustomNumberFilterParams> {

        @Override
        public CustomNumberFilterModel recognizeFilterModel(Map<String, Object> map) {
            CustomNumberFilterModel model = new CustomNumberFilterModel();
            model.setValue(map.get("value").toString());
            return model;
        }

        @Override
        public @NonNull CustomNumberFilterParams getDefaultFilterParams() {
            return new CustomNumberFilterParams(false);
        }

        @Override
        protected @NonNull Predicate toPredicate(@NonNull CriteriaBuilder cb, @NonNull Expression<T> expression, CustomNumberFilterModel model) {
            String value = model.getValue();
            if (value == null || value.equalsIgnoreCase("All")) {
                return cb.and();
            }

            Expression<Integer> integerExpression = expression.as(Integer.class);
            Predicate predicate;
            if (value.equalsIgnoreCase("Even")) {
                predicate = cb.equal(cb.mod(integerExpression, 2), 0);
            } else {
                predicate = cb.notEqual(cb.mod(integerExpression, 2), 0);
            }

            if (this.filterParams.isIncludeNullValues()) {
                predicate = cb.or(predicate, cb.isNull(expression));
            }

            return predicate;
        }
    }

    private QueryBuilder<Trade, Long, Void> customFilterQueryBuilder(CustomNumberFilterParams params) {
        CustomNumberFilter<Integer> filter = new CustomNumberFilter<>();
        if (params != null) {
            filter.filterParams(params);
        }
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.submitterId).filter(filter).build()
                )
                .build();
    }

    private ServerSideGetRowsRequest customFilterRequest(String value) {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("submitterId", Map.of("value", value)));
        return request;
    }

    @Test
    void customFilterMatchesEvenValues() {
        var result = customFilterQueryBuilder(null).getRows(customFilterRequest("Even"));
        assertThat(tradeIds(result)).containsExactly(2L, 4L, 6L, 8L, 10L, 12L);
    }

    @Test
    void customFilterMatchesOddValues() {
        var result = customFilterQueryBuilder(null).getRows(customFilterRequest("Odd"));
        // null submitterId (5) excluded
        assertThat(tradeIds(result)).containsExactly(1L, 3L, 7L, 9L, 11L);
    }

    @Test
    void customFilterParamsAreRespected() {
        var result = customFilterQueryBuilder(new CustomNumberFilterParams(true)).getRows(customFilterRequest("Even"));
        // includeNullValues adds the null submitterId (5)
        assertThat(tradeIds(result)).containsExactly(2L, 4L, 5L, 6L, 8L, 10L, 12L);
    }

    @Test
    void customFilterInactiveValueMatchesAll() {
        var result = customFilterQueryBuilder(null).getRows(customFilterRequest("All"));
        assertThat(tradeIds(result)).hasSize(12);
    }
}
