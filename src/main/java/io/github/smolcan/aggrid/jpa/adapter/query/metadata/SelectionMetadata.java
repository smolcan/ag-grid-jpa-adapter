package io.github.smolcan.aggrid.jpa.adapter.query.metadata;

import jakarta.persistence.criteria.Selection;

import java.util.Objects;

public class SelectionMetadata {
    
    private final Selection<?> selection;
    private final boolean isGroupingSelection;
    private final boolean isPivotingSelection;
    private final boolean isAggregationSelection;

    private SelectionMetadata(Builder builder) {
        this.selection = builder.selection;
        this.isGroupingSelection = builder.isGroupingSelection;
        this.isPivotingSelection = builder.isPivotingSelection;
        this.isAggregationSelection = builder.isAggregationSelection;
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

    public static class Builder {
        private Selection<?> selection;
        private boolean isGroupingSelection;
        private boolean isPivotingSelection;
        private boolean isAggregationSelection;

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

        public SelectionMetadata build() {
            return new SelectionMetadata(this);
        }
    }
}
