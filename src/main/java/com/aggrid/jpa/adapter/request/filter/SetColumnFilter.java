package com.aggrid.jpa.adapter.request.filter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor @AllArgsConstructor
@Getter @Setter
public class SetColumnFilter extends ColumnFilter {
    private List<String> values;
}
