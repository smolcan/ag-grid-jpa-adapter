package io.github.smolcan.aggrid.jpa.adapter.filter.simple;

import jakarta.persistence.criteria.*;

public abstract class ProvidedFilterModel {
    private String filterType;

    public ProvidedFilterModel(String filterType) {
        this.filterType = filterType;
    }
    public ProvidedFilterModel() {
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
