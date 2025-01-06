package io.github.smolcan.aggrid.jpa.adapter.filter.simple;

import jakarta.persistence.criteria.*;

import java.time.LocalDateTime;

public class DateFilter extends ColumnFilter {

    private SimpleFilterModelType type;
    // YYYY-MM-DD hh:mm:ss
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    
    public DateFilter() {
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
                break;
            }
            case notEqual: {
                predicate = cb.notEqual(dateExpression, this.dateFrom);
                break;
            }
            case lessThan: {
                predicate = cb.lessThan(dateExpression, this.dateFrom);
                break;
            }
            case lessThanOrEqual: {
                predicate = cb.lessThanOrEqualTo(dateExpression, this.dateFrom);
                break;
            }
            case greaterThan: {
                predicate = cb.greaterThan(dateExpression, this.dateFrom);
                break;
            }
            case greaterThanOrEqual: {
                predicate = cb.greaterThanOrEqualTo(dateExpression, this.dateFrom);
                break;
            }
            case inRange: {
                predicate = cb.and(cb.greaterThanOrEqualTo(dateExpression, this.dateFrom), cb.lessThanOrEqualTo(dateExpression, this.dateTo));
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
}
