package io.github.smolcan.aggrid.jpa.adapter.column;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;

import java.util.function.BiFunction;

/**
 * A column backed by a database expression instead of a mapped attribute, such as {@code upper(name)}
 * or a {@code CASE}. Selecting, filtering, sorting, grouping and aggregating all work as usual.
 *
 * @param <E> the entity type the column is resolved against
 * @param <T> the column value type
 */
@AllArgsConstructor
@Builder(toBuilder = true)
public class ComputedField<E, T> implements ColumnSource<E, T> {

    /**
     * @param name the column name, which must not collide with a mapped column.
     * @return the column name.
     */
    @NonNull
    private final String name;
    /**
     * @param javaType the java type the expression evaluates to.
     * @return the java type of the column value.
     */
    @NonNull
    private final Class<T> javaType;
    /**
     * @param expressionFunction builds the expression; called once per clause, so keep it side-effect free.
     * @return the function building the expression.
     */
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
