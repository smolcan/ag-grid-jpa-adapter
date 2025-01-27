package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.NumberFilterParams;
import jakarta.persistence.criteria.*;

import java.math.BigDecimal;


public class NumberFilterModel extends SimpleFilterModel {

    private BigDecimal filter;
    private BigDecimal filterTo;
    private NumberFilterParams filterParams = NumberFilterParams.builder().build();
    
    public NumberFilterModel() {
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
                if (this.filterParams.isIncludeBlanksInEquals()) {
                    predicate = cb.or(predicate, cb.isNull(numberExpression));
                }
                break;
            }
            case notEqual: {
                predicate = cb.notEqual(numberExpression, this.filter);
                if (this.filterParams.isIncludeBlanksInNotEqual()) {
                    predicate = cb.or(predicate, cb.isNull(numberExpression));
                }
                break;
            }
            case lessThan: {
                predicate = cb.lt(numberExpression, this.filter);
                if (this.filterParams.isIncludeBlanksInLessThan()) {
                    predicate = cb.or(predicate, cb.isNull(numberExpression));
                }
                break;
            }
            case lessThanOrEqual: {
                predicate = cb.le(numberExpression, this.filter);
                if (this.filterParams.isIncludeBlanksInLessThan()) {
                    predicate = cb.or(predicate, cb.isNull(numberExpression));
                }
                break;
            }
            case greaterThan: {
                predicate = cb.gt(numberExpression, this.filter);
                if (this.filterParams.isIncludeBlanksInGreaterThan()) {
                    predicate = cb.or(predicate, cb.isNull(numberExpression));
                }
                break;
            }
            case greaterThanOrEqual: {
                predicate = cb.ge(numberExpression, this.filter);
                if (this.filterParams.isIncludeBlanksInGreaterThan()) {
                    predicate = cb.or(predicate, cb.isNull(numberExpression));
                }
                break;
            }
            case inRange: {
                if (this.filterParams.isInRangeInclusive()) {
                    predicate = cb.and(cb.ge(numberExpression, this.filter), cb.le(numberExpression, this.filterTo));
                } else {
                    predicate = cb.and(cb.gt(numberExpression, this.filter), cb.lt(numberExpression, this.filterTo));
                }
                if (this.filterParams.isIncludeBlanksInRange()) {
                    predicate = cb.or(predicate, cb.isNull(numberExpression));
                }
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected value: " + this.type);
            }
        }

        return predicate;
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

    public NumberFilterParams getFilterParams() {
        return filterParams;
    }

    public void setFilterParams(NumberFilterParams filterParams) {
        this.filterParams = filterParams;
    }
}
