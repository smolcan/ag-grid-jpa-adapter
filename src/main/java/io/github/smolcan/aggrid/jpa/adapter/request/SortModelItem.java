package io.github.smolcan.aggrid.jpa.adapter.request;

public class SortModelItem {
    private String colId;
    private SortDirection sort;
    // can not make this enum, since values are 'absolute' and 'default', but default is keyword in java
    private String type;

    public SortModelItem() {
    }

    public SortModelItem(String colId, SortDirection sort, String type) {
        this.colId = colId;
        this.sort = sort;
        this.type = type;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
