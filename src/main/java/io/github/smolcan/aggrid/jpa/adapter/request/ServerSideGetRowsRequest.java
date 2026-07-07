package io.github.smolcan.aggrid.jpa.adapter.request;


import lombok.*;

import java.util.List;
import java.util.Map;


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerSideGetRowsRequest {
    /**
     * @param startRow the first row requested (inclusive).
     * @return the first row requested.
     */
    private int startRow;
    /**
     * @param endRow the index after the last row requested (exclusive).
     * @return the index after the last row requested.
     */
    private int endRow;
    /**
     * @param rowGroupCols the columns that are currently row grouped.
     * @return the row-grouped columns.
     */
    @NonNull
    private List<ColumnVO> rowGroupCols;
    /**
     * @param valueCols the columns that have aggregations on them.
     * @return the aggregated columns.
     */
    @NonNull
    private List<ColumnVO> valueCols;
    /**
     * @param pivotCols the columns that are pivoted.
     * @return the pivoted columns.
     */
    @NonNull
    private List<ColumnVO> pivotCols;
    /**
     * @param pivotMode whether pivot mode is on.
     * @return whether pivot mode is on.
     */
    private boolean pivotMode;
    /**
     * @param groupKeys the group keys of the groups the user is viewing.
     * @return the group keys the user is viewing.
     */
    @NonNull
    private List<String> groupKeys;
    /**
     * @param filterModel the filter model (if filtering).
     * @return the filter model.
     */
    private Map<String, Object> filterModel;
    /**
     * @param sortModel the sort model (if sorting).
     * @return the sort model.
     */
    @NonNull
    private List<SortModelItem> sortModel;
    /**
     * @param externalFilter the external filter value.
     * @return the external filter value.
     */
    private Object externalFilter;
    /**
     * @param quickFilter the quick filter value.
     * @return the quick filter value.
     */
    private String quickFilter;
    /**
     * @param needsGrandTotal whether the grand total row should be computed for this request.
     * @return whether the grand total row is needed.
     */
    private boolean needsGrandTotal;

}