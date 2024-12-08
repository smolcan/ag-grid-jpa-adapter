package com.aggrid.jpa.adapter.response;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadSuccessParams<E> {
    // Data retrieved from the server as requested by the grid.
    private List<E> rowData;
    // The last row, if known, to help Infinite Scroll.
    private Integer rowCount;
    // Any extra information for the grid to associate with this load.
    private Map<String, Object> groupLevelInfo;
    // The pivot fields in the response - if provided the grid will attempt to generate secondary columns.
    private List<String> pivotResultFields;
}
