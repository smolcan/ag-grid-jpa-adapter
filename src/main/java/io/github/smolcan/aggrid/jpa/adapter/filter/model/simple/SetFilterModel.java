package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;


import java.util.ArrayList;
import java.util.List;

public class SetFilterModel extends ProvidedFilterModel {
    
    public List<String> values = new ArrayList<>();
    
    public SetFilterModel() {
        super("set");
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
