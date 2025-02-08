package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;

import java.util.List;
import java.util.Map;

public class MultiFilterModel extends ProvidedFilterModel {
    
    // Child filter models in the same order as the filters are specified in filterParams.
    private List<Map<String, Object>> filterModels = null;
    
    public MultiFilterModel() {
        super("multi");
    }

    public List<Map<String, Object>> getFilterModels() {
        return filterModels;
    }

    public void setFilterModels(List<Map<String, Object>> filterModels) {
        this.filterModels = filterModels;
    }
}
