package io.github.smolcan.aggrid.jpa.adapter.query.metadata;

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
    
    private final Selection<?> selection;
    private final boolean isGroupingSelection;
    private final boolean isPivotingSelection;
    private final boolean isAggregationSelection;
    private final boolean isServerSideGroupSelection;

    private SelectionMetadata(Builder builder) {
        this.selection = builder.selection;
        this.isGroupingSelection = builder.isGroupingSelection;
        this.isPivotingSelection = builder.isPivotingSelection;
        this.isAggregationSelection = builder.isAggregationSelection;
        this.isServerSideGroupSelection = builder.isServerSideGroupSelection;
    }

    public static Builder builder(Selection<?> selection) {
        return new Builder(selection);
    }

    public Selection<?> getSelection() {
        return selection;
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

    public static class Builder {
        private Selection<?> selection;
        private boolean isGroupingSelection;
        private boolean isPivotingSelection;
        private boolean isAggregationSelection;
        private boolean isServerSideGroupSelection;

        public Builder(Selection<?> selection) {
            Objects.requireNonNull(selection);
            this.selection = selection;
        }
        
        public Builder selection(Selection<?> selection) {
            Objects.requireNonNull(selection);
            this.selection = selection;
            return this;
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

        public SelectionMetadata build() {
            return new SelectionMetadata(this);
        }
    }
}
