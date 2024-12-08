package com.aggrid.jpa.adapter.request.filter.simple;

import com.aggrid.jpa.adapter.request.filter.FilterModel;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class NumberFilterModel implements FilterModel {
    private final String filterType = "number";
    private SimpleFilterModelType type;
    private BigDecimal filter;
    private BigDecimal filterTo;
}
