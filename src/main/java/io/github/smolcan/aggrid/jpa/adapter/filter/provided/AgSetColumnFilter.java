package io.github.smolcan.aggrid.jpa.adapter.filter.provided;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SetFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.SetFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.utils.TypeValueSynchronizer;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AgSetColumnFilter implements IProvidedFilter<SetFilterModel, SetFilterParams> {
    
    @Override
    @SuppressWarnings("unchecked")
    public SetFilterModel recognizeFilterModel(Map<String, Object> filterModel) {
        SetFilterModel setFilter = new SetFilterModel();
        setFilter.setValues((List<String>) filterModel.get("values"));
        return setFilter;
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression, SetFilterModel filterModel, SetFilterParams filterParams) {
        if (filterModel.getValues().isEmpty()) {
            // empty values, FALSE predicate
            return cb.disjunction();
        }

        Expression<?> expr = null;
        List<Object> values = new ArrayList<>(filterModel.getValues().size());
        for (String value : filterModel.getValues()) {
            var syncResult = TypeValueSynchronizer.synchronizeTypes(expression, value);
            expr = syncResult.getSynchronizedPath();
            values.add(syncResult.getSynchronizedValue());
        }

        return Objects.requireNonNull(expr).in(values);
    }
}
