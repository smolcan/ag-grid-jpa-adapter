package io.github.smolcan.aggrid.jpa.adapter.filter.simple;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MultiFilterModel extends ProvidedFilterModel {
    
    private List<ProvidedFilterModel> filterModels = null;
    
    public MultiFilterModel() {
        super("multi");
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String columnName) {
        List<Predicate> predicates = this.filterModels == null 
                ? Collections.emptyList()
                : this.filterModels
                    .stream()
                    .filter(Objects::nonNull)
                    .map(f -> f.toPredicate(cb, root, columnName))
                    .collect(Collectors.toList());
        
        return cb.and(predicates.toArray(new Predicate[0]));
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression) {
        List<Predicate> predicates = this.filterModels == null
                ? Collections.emptyList()
                : this.filterModels
                    .stream()
                    .filter(Objects::nonNull)
                    .map(f -> f.toPredicate(cb, expression))
                    .collect(Collectors.toList());

        return cb.and(predicates.toArray(new Predicate[0]));
    }

    public List<ProvidedFilterModel> getFilterModels() {
        return filterModels;
    }

    public void setFilterModels(List<ProvidedFilterModel> filterModels) {
        this.filterModels = filterModels;
    }
}
