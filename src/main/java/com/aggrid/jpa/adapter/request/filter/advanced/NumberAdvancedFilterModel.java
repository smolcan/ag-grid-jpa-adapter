package com.aggrid.jpa.adapter.request.filter.advanced;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class NumberAdvancedFilterModel implements ColumnAdvancedFilterModel {
    private final String filterType = "number";
    private String colId;
    private ScalarAdvancedFilterModelType type;
    private Number filter;
}
