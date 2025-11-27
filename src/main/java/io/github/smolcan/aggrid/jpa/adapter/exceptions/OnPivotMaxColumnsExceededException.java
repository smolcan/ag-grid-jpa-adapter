package io.github.smolcan.aggrid.jpa.adapter.exceptions;


/**
 * This exception is thrown when number of pivot columns to be generated is bigger than specified max columns
 */
public class OnPivotMaxColumnsExceededException extends RuntimeException {
    
    private final int limit;
    private final long actualColumns;
    
    public OnPivotMaxColumnsExceededException(int limit, long actualColumns) {
        super(String.format("Pivot max columns exceeded, limit: %d, actual columns: %d", limit, actualColumns));
        this.limit = limit;
        this.actualColumns = actualColumns;
    }

    public int getLimit() {
        return this.limit;
    }

    public long getActualColumns() {
        return this.actualColumns;
    }
}
