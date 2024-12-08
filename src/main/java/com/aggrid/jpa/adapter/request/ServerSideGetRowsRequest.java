package com.aggrid.jpa.adapter.request;


import com.aggrid.jpa.adapter.request.filter.ColumnFilter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Map;


@Getter @Setter
@NoArgsConstructor
public class ServerSideGetRowsRequest {
    // First row requested or undefined for all rows. 
    private int startRow;
    // Index after the last row required row or undefined for all rows.
    private int endRow;
    // Columns that are currently row grouped. 
    private List<ColumnVO> rowGroupCols = Collections.emptyList();
    // Columns that have aggregations on them.
    private List<ColumnVO> valueCols = Collections.emptyList();
    // Columns that have pivot on them.
    private List<ColumnVO> pivotCols = Collections.emptyList();
    // Defines if pivot mode is on or off.
    private boolean pivotMode;
    // What groups the user is viewing.
    private List<String> groupKeys = Collections.emptyList();
    // if filtering, what the filter model is
    private Map<String, ColumnFilter> filterModel = Collections.emptyMap();
    // if sorting, what the sort model is
    private List<SortModel> sortModel = Collections.emptyList();
}