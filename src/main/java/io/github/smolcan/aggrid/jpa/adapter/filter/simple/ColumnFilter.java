package io.github.smolcan.aggrid.jpa.adapter.filter.simple;

import jakarta.persistence.criteria.*;

public abstract class ColumnFilter {
    private String filterType;

    public ColumnFilter(String filterType) {
        this.filterType = filterType;
    }
    public ColumnFilter() {
    }

    /**
     * Generate predicate for given root and column name
     */
    public abstract Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String columnName);

    /**
     * Generate predicate for given expression
     */
    public abstract Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression);


    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }
}
