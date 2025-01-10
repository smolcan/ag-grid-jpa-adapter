package io.github.smolcan.aggrid.jpa.adapter.exceptions;

public class OnPivotMaxColumnsExceededException extends Exception {
    private final int limit;
    private final int actualColumns;
    
    public OnPivotMaxColumnsExceededException(int limit, int actualColumns) {
        super(String.format("Pivot max columns exceeded, limit: %d, actual columns: %d", limit, actualColumns));
        this.limit = limit;
        this.actualColumns = actualColumns;
    }

    public int getLimit() {
        return this.limit;
    }

    public int getActualColumns() {
        return this.actualColumns;
    }
}
