package com.aggrid.jpa.adapter.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ColumnVO {
    private String id;
    private String displayName;
    private String field;
    private AggregationFunction aggFunc;
}