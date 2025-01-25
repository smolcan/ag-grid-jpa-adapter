package io.github.smolcan.aggrid.jpa.adapter.filter.simple;

public abstract class SimpleFilterModel extends ProvidedFilterModel {
    protected SimpleFilterModelType type;
    
    public SimpleFilterModel() {
        
    }
    
    public SimpleFilterModel(String filterType) {
        super(filterType);
    }
    
    public SimpleFilterModel(String filterType, SimpleFilterModelType type) {
        super(filterType);
        this.type = type;
    }

    public SimpleFilterModelType getType() {
        return type;
    }

    public void setType(SimpleFilterModelType type) {
        this.type = type;
    }
}
