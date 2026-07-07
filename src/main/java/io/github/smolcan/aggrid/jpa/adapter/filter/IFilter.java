package io.github.smolcan.aggrid.jpa.adapter.filter;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.IFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.IFilterParams;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;

/**
 * General interface for each available filter
 * @param <FM>  model the filter receives
 * @param <FP>  filter params for filter
 */
@SuppressWarnings("java:S119")
public abstract class IFilter<T, FM extends IFilterModel, FP extends IFilterParams> {
    @Getter
    protected FP filterParams = this.getDefaultFilterParams();

    /**
     * From map creates specific filter model
     * @param filterModel   filter model as Map
     * @return              mapped filter model
     */
    public abstract FM recognizeFilterModel(Map<String, Object> filterModel);

    /**
     * Provide filter params with default values
     * @return default filter params
     */
    @NonNull
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
    @NonNull
    public Predicate toPredicate(@NonNull CriteriaBuilder cb, @NonNull Expression<T> expression, Map<String, Object> filterModel) {
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
    @NonNull
    protected abstract Predicate toPredicate(@NonNull CriteriaBuilder cb, @NonNull Expression<T> expression, FM filterModel);

    @NonNull
    public IFilter<T, FM, FP> filterParams(@NonNull FP filterParams) {
        this.filterParams = filterParams;
        return this;
    }
}
