package io.github.smolcan.aggrid.jpa.adapter.request;


import java.util.Collections;
import java.util.List;
import java.util.Map;


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
    private Map<String, Object> filterModel;
    // if sorting, what the sort model is
    private List<SortModelItem> sortModel = Collections.emptyList();
    // external filter value
    private Object externalFilter;
    // quick filter value
    private String quickFilter;

    public ServerSideGetRowsRequest() {
    }

    public ServerSideGetRowsRequest(int startRow, int endRow, List<ColumnVO> rowGroupCols, List<ColumnVO> valueCols, List<ColumnVO> pivotCols, boolean pivotMode, List<String> groupKeys, Map<String, Object> filterModel, List<SortModelItem> sortModel) {
        this.startRow = startRow;
        this.endRow = endRow;
        this.rowGroupCols = rowGroupCols;
        this.valueCols = valueCols;
        this.pivotCols = pivotCols;
        this.pivotMode = pivotMode;
        this.groupKeys = groupKeys;
        this.filterModel = filterModel;
        this.sortModel = sortModel;
    }

    public int getStartRow() {
        return startRow;
    }

    public void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    public int getEndRow() {
        return endRow;
    }

    public void setEndRow(int endRow) {
        this.endRow = endRow;
    }

    public List<ColumnVO> getRowGroupCols() {
        return rowGroupCols;
    }

    public void setRowGroupCols(List<ColumnVO> rowGroupCols) {
        this.rowGroupCols = rowGroupCols;
    }

    public List<ColumnVO> getValueCols() {
        return valueCols;
    }

    public void setValueCols(List<ColumnVO> valueCols) {
        this.valueCols = valueCols;
    }

    public List<ColumnVO> getPivotCols() {
        return pivotCols;
    }

    public void setPivotCols(List<ColumnVO> pivotCols) {
        this.pivotCols = pivotCols;
    }

    public boolean isPivotMode() {
        return pivotMode;
    }

    public void setPivotMode(boolean pivotMode) {
        this.pivotMode = pivotMode;
    }

    public List<String> getGroupKeys() {
        return groupKeys;
    }

    public void setGroupKeys(List<String> groupKeys) {
        this.groupKeys = groupKeys;
    }

    public Map<String, Object> getFilterModel() {
        return filterModel;
    }

    public void setFilterModel(Map<String, Object> filterModel) {
        this.filterModel = filterModel;
    }

    public List<SortModelItem> getSortModel() {
        return sortModel;
    }

    public void setSortModel(List<SortModelItem> sortModel) {
        this.sortModel = sortModel;
    }

    public Object getExternalFilter() {
        return externalFilter;
    }

    public void setExternalFilter(Object externalFilter) {
        this.externalFilter = externalFilter;
    }

    public String getQuickFilter() {
        return quickFilter;
    }

    public void setQuickFilter(String quickFilter) {
        this.quickFilter = quickFilter;
    }
}