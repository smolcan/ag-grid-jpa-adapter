package io.github.smolcan.aggrid.jpa.adapter.filter.simple;

import io.github.smolcan.aggrid.jpa.adapter.utils.TypeValueSynchronizer;
import jakarta.persistence.criteria.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SetFilterModel extends ProvidedFilterModel {
    
    public List<String> values = new ArrayList<>();
    
    public SetFilterModel() {
        super("set");
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String columnName) {
        if (this.values.isEmpty()) {
            // empty values, FALSE predicate
            return cb.disjunction();
        }
        
        Path<?> path = root.get(columnName);
        return this.toPredicate(cb, path);
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression) {
        if (this.values.isEmpty()) {
            // empty values, FALSE predicate
            return cb.disjunction();
        }

        Expression<?> expr = null;
        List<Object> values = new ArrayList<>(this.values.size());
        for (String value : this.values) {
            var syncResult = TypeValueSynchronizer.synchronizeTypes(expression, value);
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
