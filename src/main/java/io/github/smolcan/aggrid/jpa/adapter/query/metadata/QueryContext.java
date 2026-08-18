package io.github.smolcan.aggrid.jpa.adapter.query.metadata;


import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;
import lombok.*;

import java.util.*;

/**
 * QueryContext is a metadata container used to inspect 
 * JPA query components at runtime.
 * <p>
 * It wraps various elements of a JPA query (like selections, filters, grouping, etc.)
 * using custom metadata classes to provide extended insight and processing capabilities
 * during dynamic query generation or analysis.
 * </p>
 */
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
public class QueryContext<E> {
    
    @NonNull
    private final CriteriaBuilder criteriaBuilder;
    @NonNull
    private final AbstractQuery<?> query;
    @NonNull
    private final Root<E> root;
    
    @NonNull
    @Builder.Default
    private List<SelectionMetadata> selections = new ArrayList<>();
    @NonNull
    @Builder.Default
    private List<WherePredicateMetadata> wherePredicates = new ArrayList<>();
    @NonNull
    @Builder.Default
    private List<GroupingMetadata> grouping = new ArrayList<>();
    @NonNull
    @Builder.Default
    private List<HavingMetadata> having = new ArrayList<>();
    @NonNull
    @Builder.Default
    private List<OrderMetadata> orders = new ArrayList<>();
    private int firstResult;
    private int maxResults;
    @NonNull
    @Builder.Default
    private PivotingContext pivotingContext = new PivotingContext();

    public QueryContext(@NonNull CriteriaBuilder criteriaBuilder, @NonNull AbstractQuery<?> query, @NonNull Root<E> root) {
        this.criteriaBuilder = criteriaBuilder;
        this.query = query;
        this.root = root;
    }
}
