package com.aggrid.jpa.adapter.request.filter.advanced;


import com.aggrid.jpa.adapter.request.filter.FilterModel;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public interface AdvancedFilterModel extends FilterModel {
    Predicate toPredicate(CriteriaBuilder cb, Root<?> root);
}
