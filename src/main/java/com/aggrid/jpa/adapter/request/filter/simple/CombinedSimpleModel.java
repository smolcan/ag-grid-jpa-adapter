package com.aggrid.jpa.adapter.request.filter.simple;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class CombinedSimpleModel<E extends ColumnFilter> extends ColumnFilter {
    
    private JoinOperator operator;
    private List<E> conditions = new ArrayList<>();

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String columnName) {
        List<Predicate> predicates = this.conditions.stream().map(c -> c.toPredicate(cb, root, columnName)).toList();
        if (this.operator == JoinOperator.AND) {
            return cb.and(predicates.toArray(new Predicate[0]));
        } else if (this.operator == JoinOperator.OR) {
            return cb.or(predicates.toArray(new Predicate[0]));
        } else {
            throw new IllegalStateException();
        }
    }
}
