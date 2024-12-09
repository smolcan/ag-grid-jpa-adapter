package com.aggrid.jpa.adapter.request.filter.advanced;

import com.aggrid.jpa.adapter.request.filter.advanced.enums.JoinAdvancedFilterModelType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

public class JoinAdvancedFilterModel implements AdvancedFilterModel {
    private final String filterType = "join";
    private JoinAdvancedFilterModelType type;
    private List<AdvancedFilterModel> conditions;

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root) {
        List<Predicate> predicates = this.conditions.stream().map(c -> c.toPredicate(cb, root)).toList();
        if (this.type == JoinAdvancedFilterModelType.AND) {
            return cb.and(predicates.toArray(new Predicate[0]));
        } else if (this.type == JoinAdvancedFilterModelType.OR) {
            return cb.or(predicates.toArray(new Predicate[0]));
        } else {
            throw new IllegalStateException();
        }
    }
}
