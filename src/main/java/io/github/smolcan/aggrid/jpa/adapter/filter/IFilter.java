package io.github.smolcan.aggrid.jpa.adapter.filter;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.IFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.IFilterParams;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.Map;

/**
 * General interface for each available filter
 * @param <FM>  model the filter receives
 * @param <FP>  filter params for filter
 */
public abstract class IFilter<FM extends IFilterModel, FP extends IFilterParams> {
    protected FP filterParams = this.getDefaultFilterParams();

    /**
     * From map creates specific filter model
     */
    public abstract FM recognizeFilterModel(Map<String, Object> filterModel);

    /**
     * Provide filter params with default values
     */
    public abstract FP getDefaultFilterParams();

    /**
     * Generate predicate for expression
     * Automatically parses Map to filter model
     * 
     * @param cb            criteria builder
     * @param expression    expression
     * @param filterModel   filter model as Map
     * @return              predicate for expression
     */
    public Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression, Map<String, Object> filterModel) {
        FM model = this.recognizeFilterModel(filterModel);
        return this.toPredicate(cb, expression, model);
    }

    /**
     * Generate predicate for expression
     * @param cb            criteria builder
     * @param expression    expression
     * @param filterModel   filter model
     * @return              predicate for expression
     */
    protected abstract Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression, FM filterModel);

    public FP getFilterParams() {
        return filterParams;
    }
    
    public IFilter<FM, FP> filterParams(FP filterParams) {
        this.filterParams = filterParams;
        return this;
    }
}
