package io.github.smolcan.aggrid.jpa.adapter.request;

public class ColumnVO {
    private String id;
    private String displayName;
    private String field;
    private String aggFunc;

    public ColumnVO(String id, String displayName, String field, String aggFunc) {
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

    public String getAggFunc() {
        return aggFunc;
    }

    public void setAggFunc(String aggFunc) {
        this.aggFunc = aggFunc;
    }
}