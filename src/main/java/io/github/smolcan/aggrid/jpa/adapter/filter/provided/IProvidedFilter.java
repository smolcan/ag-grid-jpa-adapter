package io.github.smolcan.aggrid.jpa.adapter.filter.provided;

import io.github.smolcan.aggrid.jpa.adapter.filter.IFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.ProvidedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.IFilterParams;

/**
 * Class every provided filter (not custom) should extend
 * @param <T>   the column value type the filter operates on
 * @param <FM>  model the filter receives
 * @param <FP>  filter params for filter
 */
@SuppressWarnings("java:S119")
public abstract class IProvidedFilter<T, FM extends ProvidedFilterModel, FP extends IFilterParams> extends IFilter<T, FM, FP> {
    
}
