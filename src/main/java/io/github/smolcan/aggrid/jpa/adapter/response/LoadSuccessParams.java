package io.github.smolcan.aggrid.jpa.adapter.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoadSuccessParams {
    // Data retrieved from the server as requested by the grid.
    private List<Map<String, Object>> rowData;
    // The last row, if known, to help Infinite Scroll.
    private Long rowCount;
    // Any extra information for the grid to associate with this load.
    private Map<String, Object> groupLevelInfo;
    // The pivot fields in the response - if provided the grid will attempt to generate secondary columns.
    private List<String> pivotResultFields;
    // Data for the grand total row
    private Map<String, Object> grandTotalData;
}
