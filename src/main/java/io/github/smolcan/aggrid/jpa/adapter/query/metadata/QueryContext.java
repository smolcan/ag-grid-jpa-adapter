package io.github.smolcan.aggrid.jpa.adapter.query.metadata;


import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;

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
public class QueryContext<E> {
    
    private final CriteriaBuilder criteriaBuilder;
    private final AbstractQuery<?> query;
    private final Root<E> root;
    
    private List<SelectionMetadata> selections;
    private List<WherePredicateMetadata> wherePredicates;
    private List<GroupingMetadata> grouping;
    private List<HavingMetadata> having;
    private List<OrderMetadata> orders;
    private int firstResult;
    private int maxResults;
    private PivotingContext pivotingContext = new PivotingContext();

    public QueryContext(CriteriaBuilder criteriaBuilder, AbstractQuery<?> query, Root<E> root) {
        this.criteriaBuilder = criteriaBuilder;
        this.query = query;
        this.root = root;
    }

    public CriteriaBuilder getCriteriaBuilder() {
        return criteriaBuilder;
    }

    public AbstractQuery<?> getQuery() {
        return query;
    }

    public Root<E> getRoot() {
        return root;
    }

    public List<SelectionMetadata> getSelections() {
        return selections;
    }

    public void setSelections(List<SelectionMetadata> selections) {
        this.selections = selections;
    }

    public List<WherePredicateMetadata> getWherePredicates() {
        return wherePredicates;
    }

    public void setWherePredicates(List<WherePredicateMetadata> wherePredicates) {
        this.wherePredicates = wherePredicates;
    }


    public List<GroupingMetadata> getGrouping() {
        return grouping;
    }

    public void setGrouping(List<GroupingMetadata> grouping) {
        this.grouping = grouping;
    }

    public List<HavingMetadata> getHaving() {
        return having;
    }

    public void setHaving(List<HavingMetadata> having) {
        this.having = having;
    }

    public List<OrderMetadata> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderMetadata> orders) {
        this.orders = orders;
    }

    public int getFirstResult() {
        return firstResult;
    }

    public void setFirstResult(int firstResult) {
        this.firstResult = firstResult;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public PivotingContext getPivotingContext() {
        return pivotingContext;
    }

    public void setPivotingContext(PivotingContext pivotingContext) {
        this.pivotingContext = pivotingContext;
    }
}
