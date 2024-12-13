package com.aggrid.jpa.adapter.request.filter.simple;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
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

        Path<? extends Number> path = root.get(columnName);
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
}
