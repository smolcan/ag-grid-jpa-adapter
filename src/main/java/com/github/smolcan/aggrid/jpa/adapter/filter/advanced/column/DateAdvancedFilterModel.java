package com.github.smolcan.aggrid.jpa.adapter.filter.advanced.column;

import com.github.smolcan.aggrid.jpa.adapter.filter.advanced.ColumnAdvancedFilterModel;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalDate;

public class DateAdvancedFilterModel extends ColumnAdvancedFilterModel {
    
    private ScalarAdvancedFilterModelType type;
    private LocalDate filter;
    
    public DateAdvancedFilterModel(String colId) {
        super("date", colId);
    }
    
    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root) {
        Predicate predicate;

        Expression<LocalDate> path = root.get(this.getColId()).as(LocalDate.class);
        switch (this.type) {
            case blank: {
                predicate = cb.isNull(path);
                break;
            }
            case notBlank: {
                predicate = cb.isNotNull(path);
                break;
            }
            case equals: {
                predicate = cb.equal(path, this.filter);
                break;
            }
            case notEqual: {
                predicate = cb.notEqual(path, this.filter);
                break;
            }
            case lessThan: {
                predicate = cb.lessThan(path, this.filter);
                break;
            }
            case lessThanOrEqual: {
                predicate = cb.lessThanOrEqualTo(path, this.filter);
                break;
            }
            case greaterThan: {
                predicate = cb.greaterThan(path, this.filter);
                break;
            }
            case greaterThanOrEqual: {
                predicate = cb.greaterThanOrEqualTo(path, this.filter);
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected value: " + this.type);
            }
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
