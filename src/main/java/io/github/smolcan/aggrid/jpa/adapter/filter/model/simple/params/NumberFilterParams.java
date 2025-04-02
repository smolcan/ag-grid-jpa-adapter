package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

public class NumberFilterParams extends ScalarFilterParams {
    
    public NumberFilterParams(Builder builder) {
        super(builder);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder extends ScalarFilterParams.Builder {
        
        public NumberFilterParams build() {
            return new NumberFilterParams(this);
        }
        
    }
}
