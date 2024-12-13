package com.aggrid.jpa.adapter.filter.advanced.column;

import com.aggrid.jpa.adapter.filter.advanced.ColumnAdvancedFilterModel;
import jakarta.persistence.criteria.*;

public class ObjectAdvancedFilterModel extends ColumnAdvancedFilterModel {
    
    private TextAdvancedFilterModelType type;
    private String filter;
    
    public ObjectAdvancedFilterModel(String colId) {
        super("object", colId);
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root) {
        Predicate predicate;

        Expression<String> path = root.get(this.getColId()).as(String.class);
        switch (this.type) {
            case blank -> predicate = cb.or(cb.isNull(path), cb.equal(path, ""));
            case notBlank -> predicate = cb.and(cb.isNotNull(path), cb.notEqual(path, ""));
            case equals -> predicate = cb.equal(path, this.filter);
            case notEqual -> predicate = cb.notEqual(path, filter);
            case contains -> predicate = cb.like(path, "%" + filter + "%");
            case notContains -> predicate = cb.notLike(path, "%" + filter + "%");
            case startsWith -> predicate = cb.like(path, filter + "%");
            case endsWith -> predicate = cb.like(path, "%" + filter);
            default -> throw new IllegalStateException("Unexpected value: " + this.type);
        }

        return predicate;
    }

    public TextAdvancedFilterModelType getType() {
        return type;
    }

    public void setType(TextAdvancedFilterModelType type) {
        this.type = type;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }
}
