package com.github.smolcan.aggrid.jpa.adapter.request;

public class SortModelItem {
    private String colId;
    private SortType sort;

    public SortModelItem() {
    }

    public SortModelItem(String colId, SortType sort) {
        this.colId = colId;
        this.sort = sort;
    }

    public String getColId() {
        return colId;
    }

    public void setColId(String colId) {
        this.colId = colId;
    }

    public SortType getSort() {
        return sort;
    }

    public void setSort(SortType sort) {
        this.sort = sort;
    }
}
