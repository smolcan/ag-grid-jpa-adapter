package io.github.smolcan.aggrid.jpa.adapter.filter.provided;

import io.github.smolcan.aggrid.jpa.adapter.filter.IFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.ProvidedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.IFilterParams;

/**
 * Class every provided filter (not custom) should extend
 * @param <FM>  model the filter receives
 * @param <FP>  filter params for filter
 */
public abstract class IProvidedFilter<FM extends ProvidedFilterModel, FP extends IFilterParams> extends IFilter<FM, FP> {
    
}
