package com.aggrid.jpa.adapter.request.filter.simple;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
public abstract class ColumnFilter {
    private String filterType;
    
    public abstract Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String columnName);
}
