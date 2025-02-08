package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.JoinOperator;

import java.util.ArrayList;
import java.util.List;

public class CombinedSimpleModel<E extends SimpleFilterModel> extends ProvidedFilterModel {
    
    private JoinOperator operator;
    private List<E> conditions = new ArrayList<>();

    public JoinOperator getOperator() {
        return operator;
    }

    public void setOperator(JoinOperator operator) {
        this.operator = operator;
    }

    public List<E> getConditions() {
        return conditions;
    }

    public void setConditions(List<E> conditions) {
        this.conditions = conditions;
    }
}
