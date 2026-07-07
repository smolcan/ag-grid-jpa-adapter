package io.github.smolcan.aggrid.jpa.adapter.query.metadata;


import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.AdvancedFilterModel;
import jakarta.persistence.criteria.Predicate;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;


/**
 * Metadata wrapper for a {@link Predicate} used in the {@code WHERE} clause of a dynamic JPA query.
 */
@Getter
@Builder
public class WherePredicateMetadata {
    
    @NonNull
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
    private final AdvancedFilterModel<?> advancedFilterModel;
    private final boolean isExternalFilterPredicate;
    private final boolean isQuickFilterPredicate;
    
}
