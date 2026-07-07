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
    
    /**
     * @param predicate the {@code WHERE} predicate.
     * @return the {@code WHERE} predicate.
     */
    @NonNull
    private final Predicate predicate;

    // tree data predicate properties
    /**
     * @param isTreeDataPredicate whether this predicate is a tree-data predicate.
     * @return whether this predicate is a tree-data predicate.
     */
    private final boolean isTreeDataPredicate;

    // master/detail predicate properties
    /**
     * @param isMasterDetailPredicate whether this predicate is a master/detail predicate.
     * @return whether this predicate is a master/detail predicate.
     */
    private final boolean isMasterDetailPredicate;

    // group predicate properties
    /**
     * @param isGroupPredicate whether this predicate filters by an expanded group key.
     * @return whether this predicate filters by an expanded group key.
     */
    private final boolean isGroupPredicate;
    /**
     * @param groupKey the (converted) group key value this predicate matches.
     * @return the group key value.
     */
    private final Object groupKey;
    /**
     * @param groupCol the field of the grouped column.
     * @return the field of the grouped column.
     */
    private final String groupCol;

    // filter predicate properties
    /**
     * @param isFilterPredicate whether this predicate comes from a filter.
     * @return whether this predicate comes from a filter.
     */
    private final boolean isFilterPredicate;
    /**
     * @param isColumnFilterPredicate whether this predicate comes from a column filter.
     * @return whether this predicate comes from a column filter.
     */
    private final boolean isColumnFilterPredicate;
    /**
     * @param isAdvancedFilterPredicate whether this predicate comes from the advanced filter.
     * @return whether this predicate comes from the advanced filter.
     */
    private final boolean isAdvancedFilterPredicate;
    /**
     * @param advancedFilterModel the advanced filter model this predicate was built from.
     * @return the advanced filter model.
     */
    private final AdvancedFilterModel<?> advancedFilterModel;
    /**
     * @param isExternalFilterPredicate whether this predicate comes from the external filter.
     * @return whether this predicate comes from the external filter.
     */
    private final boolean isExternalFilterPredicate;
    /**
     * @param isQuickFilterPredicate whether this predicate comes from the quick filter.
     * @return whether this predicate comes from the quick filter.
     */
    private final boolean isQuickFilterPredicate;
    
}
