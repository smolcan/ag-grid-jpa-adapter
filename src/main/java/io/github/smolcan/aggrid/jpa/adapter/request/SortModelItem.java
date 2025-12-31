package io.github.smolcan.aggrid.jpa.adapter.request;

public class SortModelItem {
    private String colId;
    private SortDirection sort;
    private String sortType;

    public SortModelItem() {
    }

    public SortModelItem(String colId, SortDirection sort, String sortType) {
        this.colId = colId;
        this.sort = sort;
    }

    public String getColId() {
        return colId;
    }

    public void setColId(String colId) {
        this.colId = colId;
    }

    public SortDirection getSort() {
        return sort;
    }

    public void setSort(SortDirection sort) {
        this.sort = sort;
    }

    public String getSortType() {
        return sortType;
    }

    public void setSortType(String sortType) {
        this.sortType = sortType;
    }
}
