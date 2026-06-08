package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.JoinOperator;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter(onMethod_ = {@NonNull})
@Getter
public class CombinedSimpleModel<E extends SimpleFilterModel> extends ProvidedFilterModel {
    
    private JoinOperator operator;
    private List<E> conditions = new ArrayList<>();

}
