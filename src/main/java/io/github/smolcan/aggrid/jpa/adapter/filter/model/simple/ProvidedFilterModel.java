package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.IFilterModel;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class ProvidedFilterModel implements IFilterModel {
    private String filterType;

    protected ProvidedFilterModel(String filterType) {
        this.filterType = filterType;
    }
    protected ProvidedFilterModel() {
    }


}
