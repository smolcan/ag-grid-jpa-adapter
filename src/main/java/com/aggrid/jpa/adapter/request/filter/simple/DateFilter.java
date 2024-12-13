package com.aggrid.jpa.adapter.request.filter.simple;

import jakarta.persistence.criteria.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
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
            case empty, blank -> predicate = cb.isNull(path);
            case notBlank -> predicate = cb.isNotNull(path);
            case equals -> predicate = cb.equal(path, this.dateFrom);
            case notEqual -> predicate = cb.notEqual(path, this.dateFrom);
            case lessThan -> predicate = cb.lessThan(path, this.dateFrom);
            case lessThanOrEqual -> predicate = cb.lessThanOrEqualTo(path, this.dateFrom);
            case greaterThan -> predicate = cb.greaterThan(path, this.dateFrom);
            case greaterThanOrEqual -> predicate = cb.greaterThanOrEqualTo(path, this.dateFrom);
            case inRange -> predicate = cb.and(cb.greaterThanOrEqualTo(path, this.dateFrom), cb.lessThanOrEqualTo(path, this.dateTo));
            default -> throw new IllegalStateException("Unexpected value: " + this.type);
        }
        
        return predicate;
    }
}
