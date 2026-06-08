package io.github.smolcan.aggrid.jpa.adapter.query.metadata;


import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;

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
public class QueryContext<E> {
    
    private final CriteriaBuilder criteriaBuilder;
    private final AbstractQuery<?> query;
    private final Root<E> root;
    
    private List<SelectionMetadata> selections = new ArrayList<>();
    private List<WherePredicateMetadata> wherePredicates = new ArrayList<>();
    private List<GroupingMetadata> grouping = new ArrayList<>();
    private List<HavingMetadata> having = new ArrayList<>();
    private List<OrderMetadata> orders = new ArrayList<>();
    private int firstResult;
    private int maxResults;
    private PivotingContext pivotingContext = new PivotingContext();

    public QueryContext(CriteriaBuilder criteriaBuilder, AbstractQuery<?> query, Root<E> root) {
        this.criteriaBuilder = criteriaBuilder;
        this.query = query;
        this.root = root;
    }
}
