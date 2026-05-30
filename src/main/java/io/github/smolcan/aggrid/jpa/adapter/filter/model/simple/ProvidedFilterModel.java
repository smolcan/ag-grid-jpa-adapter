package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.IFilterModel;

public abstract class ProvidedFilterModel implements IFilterModel {
    private String filterType;

    protected ProvidedFilterModel(String filterType) {
        this.filterType = filterType;
    }
    protected ProvidedFilterModel() {
    }


    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public String getFilterType() {
        return filterType;
    }
}
