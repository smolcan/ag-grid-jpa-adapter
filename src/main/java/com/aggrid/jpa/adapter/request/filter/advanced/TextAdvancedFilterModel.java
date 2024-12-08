package com.aggrid.jpa.adapter.request.filter.advanced;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TextAdvancedFilterModel implements ColumnAdvancedFilterModel {
    private final String filterType = "text";
    private String colId;
    private TextAdvancedFilterModelType type;
    private String filter;
}
