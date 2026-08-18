package io.github.smolcan.aggrid.jpa.adapter.column;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;

import java.util.function.BiFunction;

@AllArgsConstructor
@Builder(toBuilder = true)
public class ComputedField<E, T> implements ColumnSource<E, T> {

    @NonNull
    private final String name;
    @NonNull
    private final Class<T> javaType;
    @NonNull
    private final BiFunction<CriteriaBuilder, Root<E>, Expression<T>> expressionFunction;
    
    @Override
    @NonNull
    public String getName() {
        return this.name;
    }

    @Override
    @NonNull
    public Class<T> getJavaType() {
        return this.javaType;
    }

    @Override
    @NonNull
    public Expression<T> getExpression(@NonNull CriteriaBuilder cb, @NonNull Root<E> root) {
        return this.expressionFunction.apply(cb, root);
    }
}
