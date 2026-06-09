package io.github.smolcan.aggrid.jpa.adapter.request;


import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerSideGetRowsRequest {
    // First row requested or undefined for all rows. 
    private int startRow;
    // Index after the last row required row or undefined for all rows.
    private int endRow;
    // Columns that are currently row grouped.
    @NonNull
    private List<ColumnVO> rowGroupCols = Collections.emptyList();
    // Columns that have aggregations on them.
    @NonNull
    private List<ColumnVO> valueCols = Collections.emptyList();
    // Columns that have pivot on them.
    @NonNull
    private List<ColumnVO> pivotCols = Collections.emptyList();
    // Defines if pivot mode is on or off.
    private boolean pivotMode;
    // What groups the user is viewing.
    @NonNull
    private List<String> groupKeys = Collections.emptyList();
    // if filtering, what the filter model is
    private Map<String, Object> filterModel;
    // if sorting, what the sort model is
    @NonNull
    private List<SortModelItem> sortModel = Collections.emptyList();
    // external filter value
    private Object externalFilter;
    // quick filter value
    private String quickFilter;
    
    private boolean needsGrandTotal;

}