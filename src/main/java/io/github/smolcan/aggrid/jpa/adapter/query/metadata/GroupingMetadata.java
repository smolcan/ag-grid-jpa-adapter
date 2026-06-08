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
    
    @NonNull
    private final Expression<?> gropingExpression;
    @NonNull
    private final String column;
    
}
