package io.github.smolcan.aggrid.jpa.adapter.filter.simple;

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
        // ensuring number compatibility
        // comparing any number types without problem, cast both to big decimal
        Expression<BigDecimal> path = root.get(columnName).as(BigDecimal.class);
        return this.toPredicate(cb, path);
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression) {
        Predicate predicate;

        // ensuring number compatibility
        // comparing any number types without problem, cast both to big decimal
        Expression<BigDecimal> numberExpression = expression.as(BigDecimal.class);
        switch (this.type) {
            case empty: case blank: {
                predicate = cb.isNull(numberExpression);
                break;
            }
            case notBlank: {
                predicate = cb.isNotNull(numberExpression);
                break;
            }
            case equals: {
                predicate = cb.equal(numberExpression, this.filter);
                break;
            }
            case notEqual: {
                predicate = cb.notEqual(numberExpression, this.filter);
                break;
            }
            case lessThan: {
                predicate = cb.lt(numberExpression, this.filter);
                break;
            }
            case lessThanOrEqual: {
                predicate = cb.le(numberExpression, this.filter);
                break;
            }
            case greaterThan: {
                predicate = cb.gt(numberExpression, this.filter);
                break;
            }
            case greaterThanOrEqual: {
                predicate = cb.ge(numberExpression, this.filter);
                break;
            }
            case inRange: {
                predicate = cb.and(cb.ge(numberExpression, this.filter), cb.le(numberExpression, this.filterTo));
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
