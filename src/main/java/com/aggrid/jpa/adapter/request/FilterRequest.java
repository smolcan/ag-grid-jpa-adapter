package com.aggrid.jpa.adapter.request;


import com.aggrid.jpa.adapter.request.filter.ColumnFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class FilterRequest {
    private Map<String, ColumnFilter> filterModel;
}
