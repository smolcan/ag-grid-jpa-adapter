package io.github.smolcan.aggrid.jpa.adapter.request;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SortModelItem {
    private String colId;
    private SortDirection sort;
    // can not make this enum, since values are 'absolute' and 'default', but default is keyword in java
    private String type;
}
