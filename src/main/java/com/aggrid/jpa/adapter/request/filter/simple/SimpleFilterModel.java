package com.aggrid.jpa.adapter.request.filter.simple;

import com.aggrid.jpa.adapter.request.filter.FilterModel;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public interface SimpleFilterModel extends FilterModel {
    Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String colId);
}
