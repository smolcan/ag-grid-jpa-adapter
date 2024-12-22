package com.github.smolcan.aggrid.jpa.adapter.filter.simple;

import com.github.smolcan.aggrid.jpa.adapter.utils.TypeValueSynchronizer;
import jakarta.persistence.criteria.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SetFilter extends ColumnFilter {
    
    public List<String> values = new ArrayList<>();
    
    public SetFilter() {
        super("set");
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String columnName) {
        if (this.values.isEmpty()) {
            // empty values, FALSE predicate
            return cb.disjunction();
        }
        
        Path<?> path = root.get(columnName);
        Expression<?> expr = null;
        List<Object> values = new ArrayList<>(this.values.size());
        for (String value : this.values) {
            var syncResult = TypeValueSynchronizer.synchronizeTypes(path, value);
            expr = syncResult.getSynchronizedPath(); 
            values.add(syncResult.getSynchronizedValue());
        }
        
        return Objects.requireNonNull(expr).in(values);
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
