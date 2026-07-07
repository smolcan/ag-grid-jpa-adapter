package io.github.smolcan.aggrid.jpa.adapter.request;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnVO {
    /**
     * @param id the column ID.
     * @return the column ID.
     */
    private String id;
    /**
     * @param displayName the column display name.
     * @return the column display name.
     */
    private String displayName;
    /**
     * @param field the column field.
     * @return the column field.
     */
    private String field;
    /**
     * @param aggFunc the aggregation function applied to the column.
     * @return the aggregation function applied to the column.
     */
    private String aggFunc;
}