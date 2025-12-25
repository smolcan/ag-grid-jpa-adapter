package io.github.smolcan.aggrid.jpa.adapter.request;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

import java.util.function.BiFunction;

// Default aggregation functions
@SuppressWarnings({"unchecked", "rawtypes"})
public enum AggregationFunction {
    avg(
            (cb, expr) -> cb.avg((Expression) expr)
    ),
    sum(
            (cb, expr) -> cb.sum((Expression) expr)
    ),
    min(
            (cb, expr) -> cb.least((Expression) expr)
    ),
    max(
            (cb, expr) -> cb.greatest((Expression) expr)
    ),
    count(
            CriteriaBuilder::count
    ),
    ;
    
    private final BiFunction<CriteriaBuilder, Expression<?>, Expression<?>> createAggregateFunction;

    AggregationFunction(BiFunction<CriteriaBuilder, Expression<?>, Expression<?>> createAggregateFunction) {
        this.createAggregateFunction = createAggregateFunction;
    }

    public BiFunction<CriteriaBuilder, Expression<?>, Expression<?>> getCreateAggregateFunction() {
        return createAggregateFunction;
    }
}
