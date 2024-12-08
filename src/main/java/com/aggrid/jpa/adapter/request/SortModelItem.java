package com.aggrid.jpa.adapter.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class SortModelItem {
    private String colId;
    private String sort;
}
