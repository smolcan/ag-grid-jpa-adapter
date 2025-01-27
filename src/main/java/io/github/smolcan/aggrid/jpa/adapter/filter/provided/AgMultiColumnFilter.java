package io.github.smolcan.aggrid.jpa.adapter.filter.provided;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.MultiFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.MultiFilterParams;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.Map;

public class AgMultiColumnFilter implements IProvidedFilter<MultiFilterModel, MultiFilterParams> {
    
    @Override
    public MultiFilterModel recognizeFilterModel(Map<String, Object> filterModel) {
        return null;
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression, MultiFilterModel filterModel, MultiFilterParams filterParams) {
        return null;
    }
}
