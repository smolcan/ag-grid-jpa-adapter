package io.github.smolcan.aggrid.jpa.adapter.filter.simple;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public abstract class ColumnFilter {
    private String filterType;

    public ColumnFilter(String filterType) {
        this.filterType = filterType;
    }
    public ColumnFilter() {
    }

    public abstract Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String columnName);


    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }
}
