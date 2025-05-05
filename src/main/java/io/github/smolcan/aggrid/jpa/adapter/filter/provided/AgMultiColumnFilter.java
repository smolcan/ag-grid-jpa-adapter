package io.github.smolcan.aggrid.jpa.adapter.filter.provided;

import io.github.smolcan.aggrid.jpa.adapter.filter.IFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.MultiFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.MultiFilterParams;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.*;

public class AgMultiColumnFilter extends IProvidedFilter<MultiFilterModel, MultiFilterParams> {
    
    @Override
    @SuppressWarnings("unchecked")
    public MultiFilterModel recognizeFilterModel(Map<String, Object> filterModel) {
        if (this.filterParams == null || filterModel == null) {
            return null;
        }
        
        List<Map<String, Object>> filterModels = (List<Map<String, Object>>) filterModel.get("filterModels");
        List<IFilter<?, ?>> filters = this.filterParams.getFilters();
        if (filters.size() != filterModels.size()) {
            throw new IllegalArgumentException("filters.size() != filterModels.size()");
        }
        
        MultiFilterModel multiFilterModel = new MultiFilterModel();
        multiFilterModel.setFilterModels(filterModels);
        return multiFilterModel;
    }

    @Override
    public MultiFilterParams getDefaultFilterParams() {
        return MultiFilterParams.builder().build();
    }

    @Override
    protected Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression, MultiFilterModel filterModel) {
        if (this.filterParams == null || this.filterParams.getFilters().isEmpty() || filterModel == null) {
            return cb.conjunction();
        }
        
        List<IFilter<?, ?>> filters = this.filterParams.getFilters();
        List<Map<String, Object>> filterModels = filterModel.getFilterModels();
        
        List<Predicate> predicates = new ArrayList<>(filterModels.size());
        for (int i = 0; i < filters.size(); i++) {
            IFilter<?, ?> filter = filters.get(i);
            Map<String, Object> singleFilterModel = filterModels.get(i);
            
            if (singleFilterModel != null) {
                predicates.add(filter.toPredicate(cb, expression, singleFilterModel));
            }
        }
        
        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
