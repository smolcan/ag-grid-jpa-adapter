package com.aggrid.jpa.adapter.filter.advanced.column;

import com.aggrid.jpa.adapter.filter.advanced.ColumnAdvancedFilterModel;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalDate;

public class DateStringAdvancedFilterModel extends ColumnAdvancedFilterModel {
    
    private ScalarAdvancedFilterModelType type;
    private LocalDate filter;

    public DateStringAdvancedFilterModel(String colId) {
        super("dateString", colId);
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root) {
        Predicate predicate;

        Expression<LocalDate> path = root.get(this.getColId()).as(LocalDate.class);
        switch (this.type) {
            case blank -> predicate = cb.isNull(path);
            case notBlank -> predicate = cb.isNotNull(path);
            case equals -> predicate = cb.equal(path, this.filter);
            case notEqual -> predicate = cb.notEqual(path, this.filter);
            case lessThan -> predicate = cb.lessThan(path, this.filter);
            case lessThanOrEqual -> predicate = cb.lessThanOrEqualTo(path, this.filter);
            case greaterThan -> predicate = cb.greaterThan(path, this.filter);
            case greaterThanOrEqual -> predicate = cb.greaterThanOrEqualTo(path, this.filter);
            default -> throw new IllegalStateException("Unexpected value: " + this.type);
        }

        return predicate;
    }

    public ScalarAdvancedFilterModelType getType() {
        return type;
    }

    public void setType(ScalarAdvancedFilterModelType type) {
        this.type = type;
    }

    public LocalDate getFilter() {
        return filter;
    }

    public void setFilter(LocalDate filter) {
        this.filter = filter;
    }
}
