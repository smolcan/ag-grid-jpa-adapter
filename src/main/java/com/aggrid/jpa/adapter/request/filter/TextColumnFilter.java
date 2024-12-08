package com.aggrid.jpa.adapter.request.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class TextColumnFilter extends ColumnFilter {
    private String type;
    private String filter;
}
