package com.aggrid.jpa.adapter.filter.advanced.column;

import com.aggrid.jpa.adapter.filter.advanced.ColumnAdvancedFilterModel;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;

public class NumberAdvancedFilterModel extends ColumnAdvancedFilterModel {
    
    private ScalarAdvancedFilterModelType type;
    private BigDecimal filter;
    
    public NumberAdvancedFilterModel(String colId) {
        super("number", colId);
    }
    
    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root) {
        Predicate predicate;

        // ensuring number compatibility
        // comparing any number types without problem, cast both to big decimal
        Expression<BigDecimal> path = root.get(this.getColId()).as(BigDecimal.class);
        switch (this.type) {
            case blank -> predicate = cb.isNull(path);
            case notBlank -> predicate = cb.isNotNull(path);
            case equals -> predicate = cb.equal(path, this.filter);
            case notEqual -> predicate = cb.notEqual(path, this.filter);
            case lessThan -> predicate = cb.lt(path, this.filter);
            case lessThanOrEqual -> predicate = cb.le(path, this.filter);
            case greaterThan -> predicate = cb.gt(path, this.filter);
            case greaterThanOrEqual -> predicate = cb.ge(path, this.filter);
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

    public BigDecimal getFilter() {
        return filter;
    }

    public void setFilter(BigDecimal filter) {
        this.filter = filter;
    }
}
