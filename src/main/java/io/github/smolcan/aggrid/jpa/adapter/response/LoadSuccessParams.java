package io.github.smolcan.aggrid.jpa.adapter.response;

import lombok.*;

import java.util.List;
import java.util.Map;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadSuccessParams {
    /**
     * @param rowData the row data returned to the grid.
     * @return the row data returned to the grid.
     */
    private List<Map<String, Object>> rowData;
    /**
     * @param rowCount the last row index, if known (helps infinite scroll).
     * @return the last row index, if known.
     */
    private Long rowCount;
    /**
     * @param groupLevelInfo any extra information for the grid to associate with this load.
     * @return the extra group-level information.
     */
    private Map<String, Object> groupLevelInfo;
    /**
     * @param pivotResultFields the pivot result fields; if provided, the grid generates secondary columns.
     * @return the pivot result fields.
     */
    private List<String> pivotResultFields;
    /**
     * @param grandTotalData the data for the grand total row.
     * @return the data for the grand total row.
     */
    private Map<String, Object> grandTotalData;
}
