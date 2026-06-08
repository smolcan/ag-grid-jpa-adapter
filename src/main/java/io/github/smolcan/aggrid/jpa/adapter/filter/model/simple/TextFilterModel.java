package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TextFilterModel extends SimpleFilterModel {
    
    private String filter;
    private String filterTo;
    
    public TextFilterModel() {
        super("text");
    }


}
