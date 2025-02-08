package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;


import java.time.LocalDateTime;

public class DateFilterModel extends SimpleFilterModel {

    // YYYY-MM-DD hh:mm:ss
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    
    public DateFilterModel() {
        super("date");
    }

    public LocalDateTime getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDateTime dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDateTime getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDateTime dateTo) {
        this.dateTo = dateTo;
    }
}
