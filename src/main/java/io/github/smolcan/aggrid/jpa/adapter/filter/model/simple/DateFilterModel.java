package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.DateFilterParams;
import jakarta.persistence.criteria.*;

import java.time.LocalDateTime;

public class DateFilterModel extends SimpleFilterModel {

    // YYYY-MM-DD hh:mm:ss
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private DateFilterParams filterParams = DateFilterParams.builder().build();
    
    public DateFilterModel() {
        super("date");
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String columnName) {
        Expression<LocalDateTime> path = root.get(columnName).as(LocalDateTime.class);
        return this.toPredicate(cb, path);
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression) {
        Predicate predicate;
        Expression<LocalDateTime> dateExpression = expression.as(LocalDateTime.class);
        switch (this.type) {
            case empty: case blank: {
                predicate = cb.isNull(dateExpression);
                break;
            }
            case notBlank: {
                predicate = cb.isNotNull(dateExpression);
                break;
            }
            case equals: {
                predicate = cb.equal(dateExpression, this.dateFrom);
                if (this.filterParams.isIncludeBlanksInEquals()) {
                    predicate = cb.or(predicate, cb.isNull(predicate));
                }
                break;
            }
            case notEqual: {
                predicate = cb.notEqual(dateExpression, this.dateFrom);
                if (this.filterParams.isIncludeBlanksInNotEqual()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            case lessThan: {
                predicate = cb.lessThan(dateExpression, this.dateFrom);
                if (this.filterParams.isIncludeBlanksInLessThan()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            case lessThanOrEqual: {
                predicate = cb.lessThanOrEqualTo(dateExpression, this.dateFrom);
                if (this.filterParams.isIncludeBlanksInLessThan()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            case greaterThan: {
                predicate = cb.greaterThan(dateExpression, this.dateFrom);
                if (this.filterParams.isIncludeBlanksInGreaterThan()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            case greaterThanOrEqual: {
                predicate = cb.greaterThanOrEqualTo(dateExpression, this.dateFrom);
                if (this.filterParams.isIncludeBlanksInGreaterThan()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            case inRange: {
                if (this.filterParams.isInRangeInclusive()) {
                    predicate = cb.and(cb.greaterThanOrEqualTo(dateExpression, this.dateFrom), cb.lessThanOrEqualTo(dateExpression, this.dateTo));
                } else {
                    predicate = cb.and(cb.greaterThan(dateExpression, this.dateFrom), cb.lessThan(dateExpression, this.dateTo));
                }
                if (this.filterParams.isIncludeBlanksInRange()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected value: " + this.type);
            }
        }

        return predicate;
    }

    public LocalDateTime getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDateTime dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDateTime getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDateTime dateTo) {
        this.dateTo = dateTo;
    }

    public DateFilterParams getFilterParams() {
        return filterParams;
    }

    public void setFilterParams(DateFilterParams filterParams) {
        this.filterParams = filterParams;
    }
}
