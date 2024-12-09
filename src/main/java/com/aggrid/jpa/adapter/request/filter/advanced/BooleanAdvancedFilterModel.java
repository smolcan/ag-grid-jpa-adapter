package com.aggrid.jpa.adapter.request.filter.advanced;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BooleanAdvancedFilterModel implements ColumnAdvancedFilterModel {
    private final String filterType = "boolean";
    private String colId;
    private boolean type;

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root) {
        Path<?> path = root.get(this.colId);
        return cb.equal(path, this.type);
    }
}
