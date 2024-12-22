package com.github.smolcan.aggrid.jpa.adapter.request;

public class ColumnVO {
    private String id;
    private String displayName;
    private String field;
    private AggregationFunction aggFunc;

    public ColumnVO(String id, String displayName, String field, AggregationFunction aggFunc) {
        this.id = id;
        this.displayName = displayName;
        this.field = field;
        this.aggFunc = aggFunc;
    }
    
    public ColumnVO() {
        
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public AggregationFunction getAggFunc() {
        return aggFunc;
    }

    public void setAggFunc(AggregationFunction aggFunc) {
        this.aggFunc = aggFunc;
    }
}