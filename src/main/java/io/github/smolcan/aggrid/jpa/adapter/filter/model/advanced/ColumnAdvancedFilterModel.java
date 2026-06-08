package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Setter
@Getter
public abstract class ColumnAdvancedFilterModel extends AdvancedFilterModel {
    @NonNull
    private String colId;
    
    protected ColumnAdvancedFilterModel(@NonNull String filterType, @NonNull String colId) {
        super(filterType);
        this.colId = colId;
    }

}
