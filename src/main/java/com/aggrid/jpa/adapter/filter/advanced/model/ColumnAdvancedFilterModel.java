package com.aggrid.jpa.adapter.filter.advanced.model;

public abstract class ColumnAdvancedFilterModel extends AdvancedFilterModel {
    private String colId;
    
    public ColumnAdvancedFilterModel(String filterType, String colId) {
        super(filterType);
        this.colId = colId;
    }

    public String getColId() {
        return colId;
    }

    public void setColId(String colId) {
        this.colId = colId;
    }
}