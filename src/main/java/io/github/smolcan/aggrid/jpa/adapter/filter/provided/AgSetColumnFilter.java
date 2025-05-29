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
import java.util.stream.Collectors;

public class AgSetColumnFilter extends IProvidedFilter<SetFilterModel, SetFilterParams> {
    
    @Override
    @SuppressWarnings("unchecked")
    public SetFilterModel recognizeFilterModel(Map<String, Object> filterModel) {
        SetFilterModel setFilter = new SetFilterModel();
        setFilter.setValues((List<String>) filterModel.get("values"));
        return setFilter;
    }

    @Override
    public SetFilterParams getDefaultFilterParams() {
        return SetFilterParams.builder().build();
    }

    @Override
    protected Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression, SetFilterModel filterModel) {
        if (filterModel.getValues().isEmpty()) {
            // empty values, FALSE predicate
            return cb.disjunction();
        }
        
        boolean hasNullInValues = filterModel.getValues().stream().anyMatch(Objects::isNull);
        if (hasNullInValues && filterModel.getValues().size() == 1) {
            // only null value in values set
            return cb.isNull(expression);
        }
        
        
        Predicate predicate;
        if (expression.getJavaType().equals(String.class)) {
            // string type, basic behaviour
            Expression<String> stringExpression = this.generateExpressionFromFilterParams(cb, expression.as(String.class));
            List<Expression<String>> inExpressions = filterModel.getValues()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(v -> this.generateExpressionFromFilterParams(cb, cb.literal(v)))
                    .collect(Collectors.toList()); 
            
            predicate = stringExpression.in(inExpressions);
        } else {
            // other types synchronization
            Expression<?> expr = null;
            List<Object> values = new ArrayList<>(filterModel.getValues().size());
            for (String value : filterModel.getValues()) {
                if (value != null) {
                    var syncResult = TypeValueSynchronizer.synchronizeTypes(expression, value);
                    expr = syncResult.getSynchronizedPath();
                    values.add(syncResult.getSynchronizedValue());
                }
            }

            predicate = Objects.requireNonNull(expr).in(values);
        }
        
        
        if (hasNullInValues) {
            predicate = cb.or(predicate, cb.isNull(expression));
        }
        return predicate;
    }



    /**
     * With given expression, generate new expression according to filter params
     *
     * @param cb            criteria builder
     * @param expression    expression
     * @return              new expression generated from filter params
     */
    private Expression<String> generateExpressionFromFilterParams(CriteriaBuilder cb, Expression<String> expression) {
        if (this.filterParams.getTextFormatter() != null) {
            // apply custom text formatter
            expression = this.filterParams.getTextFormatter().apply(cb, expression);
        } else if (!this.filterParams.isCaseSensitive()) {
            // custom text formatter not present, apply case-insensitive
            expression = cb.lower(expression);
        }

        return expression;
    }
}
