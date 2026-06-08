package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.JoinOperator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Setter
@Getter
public class JoinAdvancedFilterModel extends AdvancedFilterModel {

    @Setter(onMethod_ = {@NonNull})
    private JoinOperator type;
    @NonNull
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

}
