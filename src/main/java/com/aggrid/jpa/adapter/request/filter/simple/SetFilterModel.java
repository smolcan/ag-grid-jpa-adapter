package com.aggrid.jpa.adapter.request.filter.simple;

import com.aggrid.jpa.adapter.request.filter.FilterModel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class SetFilterModel implements FilterModel {
    private final String filterType = "set";
    private List<String> values = new ArrayList<>();
}
