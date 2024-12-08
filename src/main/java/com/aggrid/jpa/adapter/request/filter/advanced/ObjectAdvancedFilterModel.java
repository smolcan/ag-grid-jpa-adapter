package com.aggrid.jpa.adapter.request.filter.advanced;

public class ObjectAdvancedFilterModel implements ColumnAdvancedFilterModel {
    private final String filterType = "object";
    private String colId;
    private TextAdvancedFilterModelType type;
    private String filter;
}
