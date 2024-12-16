package com.aggrid.jpa.adapter.filter.simple.model;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MultiFilter extends ColumnFilter {
    
    private List<ColumnFilter> filterModels = null;
    
    public MultiFilter() {
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

    public List<ColumnFilter> getFilterModels() {
        return filterModels;
    }

    public void setFilterModels(List<ColumnFilter> filterModels) {
        this.filterModels = filterModels;
    }
}
