package io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.JoinOperator;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.CombinedSimpleModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.IFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.IProvidedFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class SimpleFilter<FM extends SimpleFilterModel, FP extends IFilterParams> implements IProvidedFilter<FM, FP> {
    
    
    public boolean isCombinedFilter(Map<String, Object> filterModel) {
        return filterModel != null && filterModel.containsKey("conditions") && filterModel.containsKey("operator");
    }
    
    @SuppressWarnings("unchecked")
    public CombinedSimpleModel<FM> getCombinedFilter(Map<String, Object> filterModel) {
        if (!this.isCombinedFilter(filterModel)) {
            return null;
        }
        
        CombinedSimpleModel<FM> combinedSimpleModel = new CombinedSimpleModel<>();
        combinedSimpleModel.setOperator(JoinOperator.valueOf(filterModel.get("operator").toString()));
        combinedSimpleModel.setConditions(((List<Map<String, Object>>) filterModel.get("conditions")).stream().map(this::recognizeFilterModel).collect(Collectors.toList()));
        return combinedSimpleModel;
    }
    
    public Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression, CombinedSimpleModel<FM> combinedSimpleModel, FP fp) {
        List<Predicate> predicates = combinedSimpleModel.getConditions().stream().map(c -> this.toPredicate(cb, expression, c, fp)).collect(Collectors.toList());
        if (combinedSimpleModel.getOperator() == JoinOperator.AND) {
            return cb.and(predicates.toArray(new Predicate[0]));
        } else if (combinedSimpleModel.getOperator() == JoinOperator.OR) {
            return cb.or(predicates.toArray(new Predicate[0]));
        } else {
            throw new IllegalStateException();
        }
    }
}
