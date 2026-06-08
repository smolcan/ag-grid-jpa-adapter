package io.github.smolcan.aggrid.jpa.adapter.query.metadata;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Selection;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;


/**
 * Metadata wrapper for a JPA {@link Selection}, used during dynamic query construction and analysis.
 * <p>
 * This class captures the role of a selection within a query — for example, whether it is part of 
 * grouping, pivoting, or aggregation. It also enables reuse of previously defined selection expressions 
 * for operations like filtering and ordering, avoiding redundant creation and ensuring consistency 
 * across the query lifecycle.
 * </p>
 */
@Getter
@Builder
public class SelectionMetadata {
    
    @NonNull
    private final String alias;
    @NonNull
    private final Expression<?> expression;
    private final boolean isGroupingSelection;
    private final boolean isPivotingSelection;
    private final boolean isAggregationSelection;
    private final boolean isServerSideGroupSelection;
    private final boolean isChildCountSelection;

}
