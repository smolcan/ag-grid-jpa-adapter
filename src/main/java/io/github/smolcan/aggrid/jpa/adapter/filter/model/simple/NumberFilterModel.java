package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;

import java.math.BigDecimal;


public class NumberFilterModel extends SimpleFilterModel {

    private BigDecimal filter;
    private BigDecimal filterTo;
    
    public NumberFilterModel() {
        super("number");
    }

    public BigDecimal getFilter() {
        return filter;
    }

    public void setFilter(BigDecimal filter) {
        this.filter = filter;
    }

    public BigDecimal getFilterTo() {
        return filterTo;
    }

    public void setFilterTo(BigDecimal filterTo) {
        this.filterTo = filterTo;
    }

}
