package com.aggrid.jpa.adapter.request.filter.simple;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
        
        Path<?> path = root.get(columnName);
        switch (this.type) {
            case empty, blank -> predicate = cb.isNull(path);
            case notBlank -> predicate = cb.isNotNull(path);
            case equals -> predicate = cb.equal(path, this.dateFrom);
            case notEqual -> predicate = cb.notEqual(path, this.dateFrom);
            default -> {
                // todo: rest of the types, handle cast
                throw new IllegalStateException("Unexpected value: " + this.type);
            }
        }
        
        return predicate;
    }
}
