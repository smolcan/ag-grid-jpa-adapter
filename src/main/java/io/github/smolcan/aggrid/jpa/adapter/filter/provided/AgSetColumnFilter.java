package io.github.smolcan.aggrid.jpa.adapter.filter.provided;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SetFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.SetFilterParams;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class AgSetColumnFilter<T> extends IProvidedFilter<T, SetFilterModel, SetFilterParams> {

    public static AgSetStringColumnFilter forString() {
        return new AgSetStringColumnFilter();
    }

    public static <N extends Number> AgSetNumberColumnFilter<N> forNumber() {
        return new AgSetNumberColumnFilter<>();
    }

    public static AgSetUUIDColumnFilter forUUID() {
        return new AgSetUUIDColumnFilter();
    }

    public static <E extends Enum<E>> AgSetEnumColumnFilter<E> forEnum(Class<E> type) {
        return new AgSetEnumColumnFilter<>(type);
    }

    public static AgSetBooleanColumnFilter forBoolean() {
        return new AgSetBooleanColumnFilter();
    }

    public static AgSetDateColumnFilter forDate() {
        return new AgSetDateColumnFilter();
    }

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
    protected Predicate toPredicate(CriteriaBuilder cb, Expression<T> expression, SetFilterModel filterModel) {
        if (filterModel.getValues().isEmpty()) {
            // empty values, FALSE predicate
            return cb.disjunction();
        }

        boolean hasNullInValues = filterModel.getValues().stream().anyMatch(Objects::isNull);
        if (hasNullInValues && filterModel.getValues().size() == 1) {
            // only null value in values set
            return cb.isNull(expression);
        }

        // IN predicate over the non-null selected values
        Expression<T> columnExpression = this.modifyColumnExpression(cb, expression);
        List<Expression<T>> valueExpressions = filterModel.getValues()
                .stream()
                .filter(Objects::nonNull)
                .map(value -> this.parseValueToExpression(cb, value))
                .collect(Collectors.toList());
        Predicate predicate = columnExpression.in(valueExpressions);
        
        if (hasNullInValues) {
            predicate = cb.or(predicate, cb.isNull(expression));
        }
        return predicate;
    }
    
    protected Expression<T> modifyColumnExpression(@NonNull CriteriaBuilder cb, @NonNull Expression<T> expression) {
        return expression;
    }

    protected abstract Expression<T> parseValueToExpression(CriteriaBuilder cb, String value);


    public static class AgSetStringColumnFilter extends AgSetColumnFilter<String> {

        @Override
        protected Expression<String> modifyColumnExpression(CriteriaBuilder cb, Expression<String> expression) {
            return this.generateExpressionFromFilterParams(cb, expression);
        }

        @Override
        protected Expression<String> parseValueToExpression(CriteriaBuilder cb, String value) {
            return this.generateExpressionFromFilterParams(cb, cb.literal(value));
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


    public static class AgSetNumberColumnFilter<N extends Number> extends AgSetColumnFilter<N> {

        @Override
        @SuppressWarnings("unchecked")
        protected Expression<N> parseValueToExpression(CriteriaBuilder cb, String value) {
            return (Expression<N>) cb.literal(new BigDecimal(value));
        }
    }


    public static class AgSetUUIDColumnFilter extends AgSetColumnFilter<UUID> {

        @Override
        protected Expression<UUID> parseValueToExpression(CriteriaBuilder cb, String value) {
            return cb.literal(UUID.fromString(value));
        }
    }


    public static class AgSetEnumColumnFilter<E extends Enum<E>> extends AgSetColumnFilter<E> {

        private final Class<E> enumType;

        public AgSetEnumColumnFilter(@NonNull Class<E> enumType) {
            this.enumType = enumType;
        }

        @Override
        protected Expression<E> parseValueToExpression(CriteriaBuilder cb, String value) {
            return cb.literal(Enum.valueOf(this.enumType, value));
        }
    }


    public static class AgSetBooleanColumnFilter extends AgSetColumnFilter<Boolean> {

        @Override
        protected Expression<Boolean> parseValueToExpression(CriteriaBuilder cb, String value) {
            return cb.literal(parseBoolean(value));
        }

        private static Boolean parseBoolean(String value) {
            if ("true".equalsIgnoreCase(value)) {
                return Boolean.TRUE;
            } else if ("false".equalsIgnoreCase(value)) {
                return Boolean.FALSE;
            } else {
                throw new IllegalArgumentException("Cannot parse boolean value: " + value);
            }
        }
    }


    public static class AgSetDateColumnFilter extends AgSetColumnFilter<LocalDate> {

        @Override
        protected Expression<LocalDate> parseValueToExpression(CriteriaBuilder cb, String value) {
            // ag-grid sends set filter dates in ISO format (yyyy-MM-dd), which LocalDate.parse handles directly
            return cb.literal(LocalDate.parse(value));
        }
    }

}
