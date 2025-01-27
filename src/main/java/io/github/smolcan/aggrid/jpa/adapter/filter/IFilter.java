package io.github.smolcan.aggrid.jpa.adapter.filter;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.IFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.IFilterParams;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.Map;

public interface IFilter<FM extends IFilterModel, FP extends IFilterParams> {
    
    FM recognizeFilterModel(Map<String, Object> filterModel);
    Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression, FM filterModel, FP filterParams);
    
}
