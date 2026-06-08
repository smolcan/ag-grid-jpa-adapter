package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


@Setter
@Getter
public class NumberFilterModel extends SimpleFilterModel {

    private BigDecimal filter;
    private BigDecimal filterTo;
    
    public NumberFilterModel() {
        super("number");
    }

}
