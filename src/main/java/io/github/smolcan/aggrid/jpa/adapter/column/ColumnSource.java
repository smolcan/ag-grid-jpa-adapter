package io.github.smolcan.aggrid.jpa.adapter.column;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;

public interface ColumnSource<E, T> {

    @NonNull
    String getName();
    
    @NonNull 
    Class<T> getJavaType();

    @NonNull
    Expression<T> getExpression(@NonNull CriteriaBuilder cb, @NonNull Root<E> root);
    
}
