package com.github.smolcan.aggrid.jpa.adapter.filter.simple;

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
            case empty: case blank: {
                predicate = cb.or(cb.isNull(path), cb.equal(path, ""));
                break;
            }
            case notBlank: {
                predicate = cb.and(cb.isNotNull(path), cb.notEqual(path, ""));
                break;
            }
            case equals: {
                predicate = cb.equal(path, filter);
                break;
            }
            case notEqual: {
                predicate = cb.notEqual(path, filter);
                break;
            }
            case contains: {
                predicate = cb.like(path, "%" + filter + "%");
                break;
            }
            case notContains: {
                predicate = cb.notLike(path, "%" + filter + "%");
                break;
            }
            case startsWith: {
                predicate = cb.like(path, filter + "%");
                break;
            }
            case endsWith: {
                predicate = cb.like(path, "%" + filter);
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected value: " + this.type);
            }
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
