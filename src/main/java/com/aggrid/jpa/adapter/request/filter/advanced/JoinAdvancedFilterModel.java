package com.aggrid.jpa.adapter.request.filter.advanced;

import java.util.List;

public class JoinAdvancedFilterModel implements AdvancedFilterModel {
    private final String filterType = "join";
    private JoinAdvancedFilterModelType type;
    private List<AdvancedFilterModel> conditions;
}
