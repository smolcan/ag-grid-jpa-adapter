package com.github.smolcan.aggrid.jpa.adapter.filter.advanced;

import com.github.smolcan.aggrid.jpa.adapter.filter.JoinOperator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JoinAdvancedFilterModel extends AdvancedFilterModel {
    
    private JoinOperator type;
    private List<AdvancedFilterModel> conditions = new ArrayList<>();
    
    public JoinAdvancedFilterModel() {
        super("join");
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root) {
        List<Predicate> predicates = this.conditions.stream().map(c -> c.toPredicate(cb, root)).collect(Collectors.toList());
        if (this.type == JoinOperator.AND) {
            return cb.and(predicates.toArray(new Predicate[0]));
        } else if (this.type == JoinOperator.OR) {
            return cb.or(predicates.toArray(new Predicate[0]));
        } else {
            throw new IllegalStateException();
        }
    }

    public JoinOperator getType() {
        return type;
    }

    public void setType(JoinOperator type) {
        this.type = type;
    }

    public List<AdvancedFilterModel> getConditions() {
        return conditions;
    }

    public void setConditions(List<AdvancedFilterModel> conditions) {
        this.conditions = conditions;
    }
}
