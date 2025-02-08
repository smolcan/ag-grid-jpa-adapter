package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;


public class TextFilterModel extends SimpleFilterModel {
    
    private String filter;
    private String filterTo;
    
    public TextFilterModel() {
        super("text");
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getFilterTo() {
        return filterTo;
    }

    public void setFilterTo(String filterTo) {
        this.filterTo = filterTo;
    }


}
