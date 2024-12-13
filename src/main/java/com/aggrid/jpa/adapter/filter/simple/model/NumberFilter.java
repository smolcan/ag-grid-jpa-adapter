package com.aggrid.jpa.adapter.filter.simple.model;

import jakarta.persistence.criteria.*;

import java.math.BigDecimal;


public class NumberFilter extends ColumnFilter {

    private SimpleFilterModelType type;
    private BigDecimal filter;
    private BigDecimal filterTo;
    
    public NumberFilter() {
        super("number");
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String columnName) {
        Predicate predicate;

        // ensuring number compatibility
        // comparing any number types without problem, cast both to big decimal
        Expression<BigDecimal> path = root.get(columnName).as(BigDecimal.class);
        switch (this.type) {
            case empty, blank -> predicate = cb.isNull(path);
            case notBlank -> predicate = cb.isNotNull(path);
            case equals -> predicate = cb.equal(path, this.filter);
            case notEqual -> predicate = cb.notEqual(path, this.filter);
            case lessThan -> predicate = cb.lt(path, this.filter);
            case lessThanOrEqual -> predicate = cb.le(path, this.filter);
            case greaterThan -> predicate = cb.gt(path, this.filter);
            case greaterThanOrEqual -> predicate = cb.ge(path, this.filter);
            case inRange -> predicate = cb.and(cb.ge(path, this.filter), cb.le(path, this.filterTo));
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

    public BigDecimal getFilter() {
        return filter;
    }

    public void setFilter(BigDecimal filter) {
        this.filter = filter;
    }

    public BigDecimal getFilterTo() {
        return filterTo;
    }

    public void setFilterTo(BigDecimal filterTo) {
        this.filterTo = filterTo;
    }
}
