package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class SimpleFilterModel extends ProvidedFilterModel {
    protected SimpleFilterModelType type;
    
    protected SimpleFilterModel() {
        
    }

    protected SimpleFilterModel(String filterType) {
        super(filterType);
    }

    protected SimpleFilterModel(String filterType, SimpleFilterModelType type) {
        super(filterType);
        this.type = type;
    }

}
