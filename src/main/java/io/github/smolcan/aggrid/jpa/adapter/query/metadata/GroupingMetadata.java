package io.github.smolcan.aggrid.jpa.adapter.query.metadata;


import jakarta.persistence.criteria.Expression;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Metadata for a grouping expression, including the associated column name.
 */
@Getter
@Builder
public class GroupingMetadata {
    
    /**
     * @param gropingExpression the JPA expression to group by.
     * @return the grouping expression.
     */
    @NonNull
    private final Expression<?> gropingExpression;
    /**
     * @param column the name of the grouped column.
     * @return the name of the grouped column.
     */
    @NonNull
    private final String column;
    
}
