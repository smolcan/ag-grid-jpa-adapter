package com.aggrid.jpa.adapter.request.filter.simple;


import com.aggrid.jpa.adapter.request.filter.FilterModel;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TextFilterModel implements FilterModel {
    private final String filterType = "text";
    private SimpleFilterModelType type;
    private String filter;
    private String filterTo;
}
