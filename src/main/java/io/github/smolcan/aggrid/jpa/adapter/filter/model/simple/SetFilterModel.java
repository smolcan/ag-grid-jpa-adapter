package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;


import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class SetFilterModel extends ProvidedFilterModel {
    
    @NonNull
    private List<String> values = new ArrayList<>();
    
    public SetFilterModel() {
        super("set");
    }

}
