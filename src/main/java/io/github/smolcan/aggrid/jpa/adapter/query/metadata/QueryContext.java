package io.github.smolcan.aggrid.jpa.adapter.query.metadata;


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
public class QueryContext {
    
    private Map<String, SelectionMetadata> selections = new HashMap<>();
    private List<WherePredicateMetadata> wherePredicates = new ArrayList<>();
    private Map<String, GroupingMetadata> grouping = new LinkedHashMap<>();
    private List<HavingMetadata> having = new ArrayList<>();
    private List<OrderMetadata> orders = new ArrayList<>();
    private int firstResult;
    private int maxResults;
    private PivotingContext pivotingContext = new PivotingContext();


    public Map<String, SelectionMetadata> getSelections() {
        return selections;
    }

    public void setSelections(Map<String, SelectionMetadata> selections) {
        this.selections = selections;
    }

    public List<WherePredicateMetadata> getWherePredicates() {
        return wherePredicates;
    }

    public void setWherePredicates(List<WherePredicateMetadata> wherePredicates) {
        this.wherePredicates = wherePredicates;
    }

    public Map<String, GroupingMetadata> getGrouping() {
        return grouping;
    }

    public void setGrouping(Map<String, GroupingMetadata> grouping) {
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
