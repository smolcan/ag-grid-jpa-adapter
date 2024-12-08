package com.aggrid.jpa.adapter.request.filter.advanced;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BooleanAdvancedFilterModel implements ColumnAdvancedFilterModel {
    private final String filterType = "boolean";
    private String colId;
    private boolean type;
}
