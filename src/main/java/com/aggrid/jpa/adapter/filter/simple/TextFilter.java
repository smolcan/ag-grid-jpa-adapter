package com.aggrid.jpa.adapter.filter.simple;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class TextFilter extends ColumnFilter {
    
    private SimpleFilterModelType type;
    private String filter;
    private String filterTo;
    
    public TextFilter() {
        super("text");
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String columnName) {
        Predicate predicate;
        
        Path<String> path = root.get(columnName);
        switch (this.type) {
            case empty, blank -> predicate = cb.or(cb.isNull(path), cb.equal(path, ""));
            case notBlank -> predicate = cb.and(cb.isNotNull(path), cb.notEqual(path, ""));
            case equals -> predicate = cb.equal(path, filter);
            case notEqual -> predicate = cb.notEqual(path, filter);
            case contains -> predicate = cb.like(path, "%" + filter + "%");
            case notContains -> predicate = cb.notLike(path, "%" + filter + "%");
            case startsWith -> predicate = cb.like(path, filter + "%");
            case endsWith -> predicate = cb.like(path, "%" + filter);
            default -> throw new IllegalStateException("Unexpected value: " + this.type);
        }
        
        return predicate;
    }

    public SimpleFilterModelType getType() {
        return type;
    }

    public void setType(SimpleFilterModelType type) {
        this.type = type;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getFilterTo() {
        return filterTo;
    }

    public void setFilterTo(String filterTo) {
        this.filterTo = filterTo;
    }
}
