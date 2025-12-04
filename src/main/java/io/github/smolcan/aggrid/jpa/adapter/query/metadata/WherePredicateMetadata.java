package io.github.smolcan.aggrid.jpa.adapter.query.metadata;


import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.AdvancedFilterModel;
import jakarta.persistence.criteria.Predicate;

import java.util.Objects;


/**
 * Metadata wrapper for a {@link Predicate} used in the {@code WHERE} clause of a dynamic JPA query.
 */
public class WherePredicateMetadata {
    
    private final Predicate predicate;
    
    // tree data predicate properties
    private final boolean isTreeDataPredicate;
    
    // master/detail predicate properties
    private final boolean isMasterDetailPredicate;
    
    // group predicate properties
    private final boolean isGroupPredicate;
    private final Object groupKey;
    private final String groupCol;
    
    // filter predicate properties
    private final boolean isFilterPredicate;
    private final boolean isColumnFilterPredicate;
    private final boolean isAdvancedFilterPredicate;
    private final AdvancedFilterModel advancedFilterModel;
    private final boolean isExternalFilterPredicate;

    private WherePredicateMetadata(Builder builder) {
        this.predicate = builder.predicate;
        this.isTreeDataPredicate = builder.isTreeDataPredicate;
        this.isMasterDetailPredicate = builder.isMasterDetailPredicate;
        this.isGroupPredicate = builder.isGroupPredicate;
        this.groupKey = builder.groupKey;
        this.groupCol = builder.groupCol;
        this.isFilterPredicate = builder.isFilterPredicate;
        this.isColumnFilterPredicate = builder.isColumnFilterPredicate;
        this.isAdvancedFilterPredicate = builder.isAdvancedFilterPredicate;
        this.advancedFilterModel = builder.advancedFilterModel;
        this.isExternalFilterPredicate = builder.isExternalFilterPredicate;
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public boolean isGroupPredicate() {
        return isGroupPredicate;
    }

    public Object getGroupKey() {
        return groupKey;
    }

    public String getGroupCol() {
        return groupCol;
    }

    public boolean isFilterPredicate() {
        return isFilterPredicate;
    }

    public boolean isColumnFilterPredicate() {
        return isColumnFilterPredicate;
    }

    public boolean isAdvancedFilterPredicate() {
        return isAdvancedFilterPredicate;
    }
    
    public boolean isTreeDataPredicate() {
        return isTreeDataPredicate;
    }

    public boolean isMasterDetailPredicate() {
        return isMasterDetailPredicate;
    }

    public boolean isExternalFilterPredicate() {
        return isExternalFilterPredicate;
    }

    public AdvancedFilterModel getAdvancedFilterModel() {
        return advancedFilterModel;
    }

    public static Builder builder(Predicate predicate) {
        return new Builder(predicate);
    }

    public static class Builder {
        private Predicate predicate;
        
        private boolean isTreeDataPredicate;

        private boolean isMasterDetailPredicate;

        private boolean isGroupPredicate;
        private Object groupKey;
        private String groupCol;

        private boolean isFilterPredicate;
        private boolean isColumnFilterPredicate;
        private boolean isAdvancedFilterPredicate;
        private AdvancedFilterModel advancedFilterModel;
        private boolean isExternalFilterPredicate;
        
        public Builder(Predicate predicate) {
            Objects.requireNonNull(predicate);
            this.predicate = predicate;
        }

        public Builder predicate(Predicate predicate) {
            Objects.requireNonNull(predicate);
            this.predicate = predicate;
            return this;
        }

        public Builder isGroupPredicate(boolean isGroupPredicate) {
            this.isGroupPredicate = isGroupPredicate;
            return this;
        }

        public Builder groupKey(Object groupKey) {
            this.groupKey = groupKey;
            return this;
        }

        public Builder groupCol(String groupCol) {
            this.groupCol = groupCol;
            return this;
        }

        public Builder isFilterPredicate(boolean isFilterPredicate) {
            this.isFilterPredicate = isFilterPredicate;
            return this;
        }

        public Builder isColumnFilterPredicate(boolean isColumnFilterPredicate) {
            this.isColumnFilterPredicate = isColumnFilterPredicate;
            return this;
        }

        public Builder isAdvancedFilterPredicate(boolean isAdvancedFilterPredicate) {
            this.isAdvancedFilterPredicate = isAdvancedFilterPredicate;
            return this;
        }

        public Builder advancedFilterModel(AdvancedFilterModel advancedFilterModel) {
            this.advancedFilterModel = advancedFilterModel;
            return this;
        }
        
        public Builder isTreeDataPredicate(boolean isTreeDataPredicate) {
            this.isTreeDataPredicate = isTreeDataPredicate;
            return this;
        }
        
        public Builder isMasterDetailPredicate(boolean isMasterDetailPredicate) {
            this.isMasterDetailPredicate = isMasterDetailPredicate;
            return this;
        }
        
        public Builder isExternalFilterPredicate(boolean isExternalFilterPredicate) {
            this.isExternalFilterPredicate = isExternalFilterPredicate;
            return this;
        }

        public WherePredicateMetadata build() {
            return new WherePredicateMetadata(this);
        }
    }
    
}
