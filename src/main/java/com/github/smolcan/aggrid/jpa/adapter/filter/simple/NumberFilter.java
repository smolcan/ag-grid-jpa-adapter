package com.github.smolcan.aggrid.jpa.adapter.filter.simple;

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
            case empty: case blank: {
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
                predicate = cb.lt(path, this.filter);
                break;
            }
            case lessThanOrEqual: {
                predicate = cb.le(path, this.filter);
                break;
            }
            case greaterThan: {
                predicate = cb.gt(path, this.filter);
                break;
            }
            case greaterThanOrEqual: {
                predicate = cb.ge(path, this.filter);
                break;
            }
            case inRange: {
                predicate = cb.and(cb.ge(path, this.filter), cb.le(path, this.filterTo));
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
