package io.github.smolcan.aggrid.jpa.adapter.query.metadata;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Selection;

import java.util.Objects;


/**
 * Metadata wrapper for a JPA {@link Selection}, used during dynamic query construction and analysis.
 * <p>
 * This class captures the role of a selection within a query — for example, whether it is part of 
 * grouping, pivoting, or aggregation. It also enables reuse of previously defined selection expressions 
 * for operations like filtering and ordering, avoiding redundant creation and ensuring consistency 
 * across the query lifecycle.
 * </p>
 */
public class SelectionMetadata {
    
    private final String alias;
    private final Expression<?> expression;
    private final boolean isGroupingSelection;
    private final boolean isPivotingSelection;
    private final boolean isAggregationSelection;
    private final boolean isServerSideGroupSelection;
    private final boolean isChildCountSelection;

    private SelectionMetadata(Builder builder) {
        this.alias = builder.alias;
        this.expression = builder.selection;
        this.isGroupingSelection = builder.isGroupingSelection;
        this.isPivotingSelection = builder.isPivotingSelection;
        this.isAggregationSelection = builder.isAggregationSelection;
        this.isServerSideGroupSelection = builder.isServerSideGroupSelection;
        this.isChildCountSelection = builder.isChildCountSelection;
    }

    public static Builder builder(Expression<?> expression, String alias) {
        return new Builder(expression, alias);
    }

    public String getAlias() {
        return alias;
    }

    public Expression<?> getExpression() {
        return expression;
    }

    public boolean isGroupingSelection() {
        return isGroupingSelection;
    }

    public boolean isPivotingSelection() {
        return isPivotingSelection;
    }

    public boolean isAggregationSelection() {
        return isAggregationSelection;
    }
    
    public boolean isServerSideGroupSelection() {
        return isServerSideGroupSelection;
    }
    
    public boolean isChildCountSelection() {
        return isChildCountSelection;
    }

    public static class Builder {
        private final String alias;
        private final Expression<?> selection;
        private boolean isGroupingSelection;
        private boolean isPivotingSelection;
        private boolean isAggregationSelection;
        private boolean isServerSideGroupSelection;
        private boolean isChildCountSelection;

        public Builder(Expression<?> selection, String alias) {
            Objects.requireNonNull(selection);
            Objects.requireNonNull(alias);
            this.selection = selection;
            this.alias = alias;
        }

        public Builder isGroupingSelection(boolean isGroupingSelection) {
            this.isGroupingSelection = isGroupingSelection;
            return this;
        }

        public Builder isPivotingSelection(boolean isPivotingSelection) {
            this.isPivotingSelection = isPivotingSelection;
            return this;
        }

        public Builder isAggregationSelection(boolean isAggregationSelection) {
            this.isAggregationSelection = isAggregationSelection;
            return this;
        }
        
        public Builder isServerSideGroupSelection(boolean isServerSideGroupSelection) {
            this.isServerSideGroupSelection = isServerSideGroupSelection;
            return this;
        }
        
        public Builder isChildCountSelection(boolean isChildCountSelection) {
            this.isChildCountSelection = isChildCountSelection;
            return this;
        }

        public SelectionMetadata build() {
            return new SelectionMetadata(this);
        }
    }
}
