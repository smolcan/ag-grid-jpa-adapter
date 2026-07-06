package io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.JoinOperator;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.CombinedSimpleModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.IFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.IProvidedFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.NonNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple provided filters: agTextColumnFilter, agNumberColumnFilter, agDateColumnFilter
 * @param <FM>  model the filter receives
 * @param <FP>  filter params for filter
 */
@SuppressWarnings("java:S119")
public abstract class SimpleFilter<T, FM extends SimpleFilterModel, FP extends IFilterParams> extends IProvidedFilter<T, FM, FP> {

    /**
     * Overrides this method because in simple filter, we must first check if filter is combined
     * and if so, create predicate from combined filter, otherwise default behaviour
     * 
     * @param cb            criteria builder
     * @param expression    expression
     * @param filterModel   filter model as Map
     * @return              predicate
     */
    @Override
    @NonNull
    public Predicate toPredicate(@NonNull CriteriaBuilder cb, @NonNull Expression<T> expression, Map<String, Object> filterModel) {
        if (this.isCombinedFilter(filterModel)) {
            CombinedSimpleModel<FM> combinedSimpleModel = this.getCombinedFilterModel(filterModel);
            return this.toCombinedPredicate(cb, expression, combinedSimpleModel);
        } else {
            FM model = this.recognizeFilterModel(filterModel);
            return this.toPredicate(cb, expression, model);
        }
    }

    /**
     * Simple filters can be combined
     * Checks map if the filter is combined
     *
     * @param filterModel   map
     * @return              is combined or not
     */
    private boolean isCombinedFilter(Map<String, Object> filterModel) {
        return filterModel != null && filterModel.containsKey("conditions") && filterModel.containsKey("operator");
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private CombinedSimpleModel<FM> getCombinedFilterModel(@NonNull Map<String, Object> filterModel) {
        CombinedSimpleModel<FM> combinedSimpleModel = new CombinedSimpleModel<>();
        combinedSimpleModel.setOperator(JoinOperator.valueOf(filterModel.get("operator").toString()));
        combinedSimpleModel.setConditions(((List<Map<String, Object>>) filterModel.get("conditions")).stream().map(this::recognizeFilterModel).collect(Collectors.toList()));
        return combinedSimpleModel;
    }
    
    @NonNull
    private Predicate toCombinedPredicate(@NonNull CriteriaBuilder cb, @NonNull Expression<T> expression, @NonNull CombinedSimpleModel<FM> combinedSimpleModel) {
        List<Predicate> predicates = combinedSimpleModel.getConditions().stream().map(c -> this.toPredicate(cb, expression, c)).collect(Collectors.toList());
        if (combinedSimpleModel.getOperator() == JoinOperator.AND) {
            return cb.and(predicates.toArray(new Predicate[0]));
        } else if (combinedSimpleModel.getOperator() == JoinOperator.OR) {
            return cb.or(predicates.toArray(new Predicate[0]));
        } else {
            throw new IllegalStateException();
        }
    }
}
