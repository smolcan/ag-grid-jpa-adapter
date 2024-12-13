package com.aggrid.jpa.adapter.filter.simple;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;

public class SetFilter extends ColumnFilter {
    
    public List<String> values = new ArrayList<>();
    
    public SetFilter() {
        super("set");
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String columnName) {
        Path<String> path = root.get(columnName);

        CriteriaBuilder.In<String> inClause = cb.in(path);
        this.values.forEach(inClause::value);
        
        return inClause;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
