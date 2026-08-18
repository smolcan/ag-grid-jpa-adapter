package io.github.smolcan.aggrid.jpa.adapter.column;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;

/**
 * Where a column reads its value from: a mapped attribute ({@link FieldPath}) or a database
 * expression ({@link ComputedField}).
 *
 * @param <E> the entity type the column is resolved against
 * @param <T> the column value type
 */
public interface ColumnSource<E, T> {

    /**
     * @return the column name, as the grid sends it and as the response field name.
     */
    @NonNull
    String getName();

    /**
     * @return the java type of the column value.
     */
    @NonNull
    Class<T> getJavaType();

    /**
     * @param cb the criteria builder.
     * @param root the root to resolve the column against.
     * @return the expression the column value is read from.
     */
    @NonNull
    Expression<T> getExpression(@NonNull CriteriaBuilder cb, @NonNull Root<E> root);

}
