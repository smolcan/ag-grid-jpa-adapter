package com.aggrid.jpa.adapter.request.filter.advanced;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DateStringAdvancedFilterModel implements ColumnAdvancedFilterModel {
    private final String filterType = "dateString";
    private String colId;
    private ScalarAdvancedFilterModelType type;
    private String filter;
    
}
