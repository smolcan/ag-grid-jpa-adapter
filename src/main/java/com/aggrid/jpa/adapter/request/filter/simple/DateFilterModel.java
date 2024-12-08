package com.aggrid.jpa.adapter.request.filter.simple;

import com.aggrid.jpa.adapter.request.filter.FilterModel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class DateFilterModel implements FilterModel {
    private final String filterType = "date";
    private SimpleFilterModelType type;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
}
