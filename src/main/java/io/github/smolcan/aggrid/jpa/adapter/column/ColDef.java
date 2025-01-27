package io.github.smolcan.aggrid.jpa.adapter.column;

import io.github.smolcan.aggrid.jpa.adapter.filter.IFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.IFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.IFilterParams;


public class ColDef<F extends IFilter<FM, FP>, FM extends IFilterModel, FP extends IFilterParams> {
    
    private final String field;
    private final F filter;
    private final FP filterParams;

    public ColDef(String field, F filter, FP filterParams) {
        this.field = field;
        this.filter = filter;
        this.filterParams = filterParams;
    }

    public String getField() {
        return field;
    }

    public F getFilter() {
        return filter;
    }

    public FP getFilterParams() {
        return filterParams;
    }

    public static void main(String[] args) {
        
    }
}
