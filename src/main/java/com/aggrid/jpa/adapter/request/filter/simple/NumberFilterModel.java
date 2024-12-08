package com.aggrid.jpa.adapter.request.filter.simple;

import com.aggrid.jpa.adapter.request.filter.FilterModel;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class NumberFilterModel implements FilterModel {
    private final String filterType = "number";
    private SimpleFilterModelType type;
    private Number filter;
    private Number filterTo;
}
