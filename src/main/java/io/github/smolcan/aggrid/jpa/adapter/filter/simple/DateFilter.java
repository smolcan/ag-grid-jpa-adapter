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
        Predicate predicate;
        
        Expression<LocalDateTime> path = root.get(columnName).as(LocalDateTime.class);
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
                predicate = cb.equal(path, this.dateFrom);
                break;
            }
            case notEqual: {
                predicate = cb.notEqual(path, this.dateFrom);
                break;
            }
            case lessThan: {
                predicate = cb.lessThan(path, this.dateFrom);
                break;
            }
            case lessThanOrEqual: {
                predicate = cb.lessThanOrEqualTo(path, this.dateFrom);
                break;
            }
            case greaterThan: {
                predicate = cb.greaterThan(path, this.dateFrom);
                break;
            }
            case greaterThanOrEqual: {
                predicate = cb.greaterThanOrEqualTo(path, this.dateFrom);
                break;
            }
            case inRange: {
                predicate = cb.and(cb.greaterThanOrEqualTo(path, this.dateFrom), cb.lessThanOrEqualTo(path, this.dateTo));
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
