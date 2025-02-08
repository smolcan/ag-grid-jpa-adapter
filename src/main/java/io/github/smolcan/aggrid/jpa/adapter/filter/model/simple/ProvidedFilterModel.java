package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.IFilterModel;

public abstract class ProvidedFilterModel implements IFilterModel {
    private String filterType;

    public ProvidedFilterModel(String filterType) {
        this.filterType = filterType;
    }
    public ProvidedFilterModel() {
    }


    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }
}
