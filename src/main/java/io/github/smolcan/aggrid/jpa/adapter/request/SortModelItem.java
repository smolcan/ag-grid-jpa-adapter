package io.github.smolcan.aggrid.jpa.adapter.request;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SortModelItem {
    /**
     * @param colId the ID of the column being sorted.
     * @return the ID of the column being sorted.
     */
    private String colId;
    /**
     * @param sort the sort direction.
     * @return the sort direction.
     */
    private SortDirection sort;
    /**
     * The sort type, e.g. {@code 'absolute'} or {@code 'default'} (kept as a String because {@code default} is a Java keyword).
     *
     * @param type the sort type.
     * @return the sort type.
     */
    private String type;
}
