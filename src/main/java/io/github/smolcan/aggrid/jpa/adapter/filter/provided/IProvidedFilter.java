package io.github.smolcan.aggrid.jpa.adapter.filter.provided;

import io.github.smolcan.aggrid.jpa.adapter.filter.IFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.ProvidedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.IFilterParams;


public interface IProvidedFilter<FM extends ProvidedFilterModel, FP extends IFilterParams> extends IFilter<FM, FP> {
    
}
