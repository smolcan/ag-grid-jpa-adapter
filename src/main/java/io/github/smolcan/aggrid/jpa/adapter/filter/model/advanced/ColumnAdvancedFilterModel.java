package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class ColumnAdvancedFilterModel extends AdvancedFilterModel {
    private String colId;
    
    protected ColumnAdvancedFilterModel(String filterType, String colId) {
        super(filterType);
        this.colId = colId;
    }

}
