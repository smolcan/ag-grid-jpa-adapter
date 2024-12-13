package com.aggrid.jpa.adapter.response;

import java.util.List;
import java.util.Map;

public class LoadSuccessParams {
    // Data retrieved from the server as requested by the grid.
    private List<Map<String, Object>> rowData;
    // The last row, if known, to help Infinite Scroll.
    private Integer rowCount;
    // Any extra information for the grid to associate with this load.
    private Map<String, Object> groupLevelInfo;
    // The pivot fields in the response - if provided the grid will attempt to generate secondary columns.
    private List<String> pivotResultFields;

    public LoadSuccessParams(List<Map<String, Object>> rowData, Integer rowCount, Map<String, Object> groupLevelInfo, List<String> pivotResultFields) {
        this.rowData = rowData;
        this.rowCount = rowCount;
        this.groupLevelInfo = groupLevelInfo;
        this.pivotResultFields = pivotResultFields;
    }
    
    public LoadSuccessParams() {
    }

    public List<Map<String, Object>> getRowData() {
        return rowData;
    }

    public void setRowData(List<Map<String, Object>> rowData) {
        this.rowData = rowData;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public Map<String, Object> getGroupLevelInfo() {
        return groupLevelInfo;
    }

    public void setGroupLevelInfo(Map<String, Object> groupLevelInfo) {
        this.groupLevelInfo = groupLevelInfo;
    }

    public List<String> getPivotResultFields() {
        return pivotResultFields;
    }

    public void setPivotResultFields(List<String> pivotResultFields) {
        this.pivotResultFields = pivotResultFields;
    }
}
