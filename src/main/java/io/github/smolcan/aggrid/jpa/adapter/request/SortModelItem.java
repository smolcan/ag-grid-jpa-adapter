package io.github.smolcan.aggrid.jpa.adapter.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SortModelItem {
    private String colId;
    private SortDirection sort;
    // can not make this enum, since values are 'absolute' and 'default', but default is keyword in java
    private String type;
}
