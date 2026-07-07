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
    
    /**
     * @param alias the selection alias (output field name).
     * @return the selection alias.
     */
    @NonNull
    private final String alias;
    /**
     * @param expression the selected JPA expression.
     * @return the selected expression.
     */
    @NonNull
    private final Expression<?> expression;
    /**
     * @param isGroupingSelection whether this selection is a grouping column.
     * @return whether this selection is a grouping column.
     */
    private final boolean isGroupingSelection;
    /**
     * @param isPivotingSelection whether this selection is a pivot column.
     * @return whether this selection is a pivot column.
     */
    private final boolean isPivotingSelection;
    /**
     * @param isAggregationSelection whether this selection is an aggregation.
     * @return whether this selection is an aggregation.
     */
    private final boolean isAggregationSelection;
    /**
     * @param isServerSideGroupSelection whether this selection is the tree-data "is group" flag.
     * @return whether this selection is the tree-data "is group" flag.
     */
    private final boolean isServerSideGroupSelection;
    /**
     * @param isChildCountSelection whether this selection is a child-count value.
     * @return whether this selection is a child-count value.
     */
    private final boolean isChildCountSelection;

}
