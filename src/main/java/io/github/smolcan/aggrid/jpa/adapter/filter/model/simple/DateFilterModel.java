package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class DateFilterModel extends SimpleFilterModel {

    // YYYY-MM-DD hh:mm:ss
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    
    public DateFilterModel() {
        super("date");
    }

}
