package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class MultiFilterModel extends ProvidedFilterModel {
    
    // Child filter models in the same order as the filters are specified in filterParams.
    private List<Map<String, Object>> filterModels = null;
    
    public MultiFilterModel() {
        super("multi");
    }

}
