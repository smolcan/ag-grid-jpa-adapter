package io.github.smolcan.aggrid.jpa.adapter.filter.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.JoinOperator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CombinedSimpleModel<E extends SimpleFilterModel> extends ProvidedFilterModel {
    
    private JoinOperator operator;
    private List<E> conditions = new ArrayList<>();

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String columnName) {
        List<Predicate> predicates = this.conditions.stream().map(c -> c.toPredicate(cb, root, columnName)).collect(Collectors.toList());
        if (this.operator == JoinOperator.AND) {
            return cb.and(predicates.toArray(new Predicate[0]));
        } else if (this.operator == JoinOperator.OR) {
            return cb.or(predicates.toArray(new Predicate[0]));
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression) {
        List<Predicate> predicates = this.conditions.stream().map(c -> c.toPredicate(cb, expression)).collect(Collectors.toList());
        if (this.operator == JoinOperator.AND) {
            return cb.and(predicates.toArray(new Predicate[0]));
        } else if (this.operator == JoinOperator.OR) {
            return cb.or(predicates.toArray(new Predicate[0]));
        } else {
            throw new IllegalStateException();
        }
    }


    public JoinOperator getOperator() {
        return operator;
    }

    public void setOperator(JoinOperator operator) {
        this.operator = operator;
    }

    public List<E> getConditions() {
        return conditions;
    }

    public void setConditions(List<E> conditions) {
        this.conditions = conditions;
    }
}
