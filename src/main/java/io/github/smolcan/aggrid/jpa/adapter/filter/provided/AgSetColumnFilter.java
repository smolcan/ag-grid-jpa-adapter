package io.github.smolcan.aggrid.jpa.adapter.filter.provided;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SetFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.SetFilterParams;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.NonNull;

import java.math.BigDecimal;
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

        // type-specific IN predicate over the non-null selected values
        Predicate predicate = this.toSetPredicate(cb, expression, filterModel);
        if (hasNullInValues) {
            predicate = cb.or(predicate, cb.isNull(expression));
        }
        return predicate;
    }
    
    protected abstract @NonNull Predicate toSetPredicate(@NonNull CriteriaBuilder cb, @NonNull Expression<T> expression, @NonNull SetFilterModel filterModel);


    public static class AgSetStringColumnFilter extends AgSetColumnFilter<String> {

        @Override
        protected Predicate toSetPredicate(CriteriaBuilder cb, Expression<String> expression, SetFilterModel filterModel) {
            Expression<String> stringExpression = this.generateExpressionFromFilterParams(cb, expression);
            List<Expression<String>> inExpressions = filterModel.getValues()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(v -> this.generateExpressionFromFilterParams(cb, cb.literal(v)))
                    .collect(Collectors.toList());

            return stringExpression.in(inExpressions);
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
        protected Predicate toSetPredicate(CriteriaBuilder cb, Expression<N> expression, SetFilterModel filterModel) {
            // column is statically known to be numeric, so parse each value into a BigDecimal
            // (lossless for any Number type, compared numerically by the database in the IN clause)
            List<BigDecimal> values = filterModel.getValues()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(BigDecimal::new)
                    .collect(Collectors.toList());

            return expression.in(values);
        }
    }


    public static class AgSetUUIDColumnFilter extends AgSetColumnFilter<UUID> {

        @Override
        protected Predicate toSetPredicate(CriteriaBuilder cb, Expression<UUID> expression, SetFilterModel filterModel) {
            // column is statically known to be a UUID, so parse each value straight into a UUID
            List<UUID> values = filterModel.getValues()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(UUID::fromString)
                    .collect(Collectors.toList());

            return expression.in(values);
        }
    }


    public static class AgSetEnumColumnFilter<E extends Enum<E>> extends AgSetColumnFilter<E> {

        private final Class<E> enumType;

        public AgSetEnumColumnFilter(Class<E> enumType) {
            this.enumType = Objects.requireNonNull(enumType);
        }

        @Override
        protected Predicate toSetPredicate(CriteriaBuilder cb, Expression<E> expression, SetFilterModel filterModel) {
            List<E> values = filterModel.getValues()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(v -> Enum.valueOf(this.enumType, v))
                    .collect(Collectors.toList());

            return expression.in(values);
        }
    }


    public static class AgSetBooleanColumnFilter extends AgSetColumnFilter<Boolean> {

        @Override
        protected Predicate toSetPredicate(CriteriaBuilder cb, Expression<Boolean> expression, SetFilterModel filterModel) {
            // column is statically known to be a Boolean, so parse each value into a Boolean
            List<Boolean> values = filterModel.getValues()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(AgSetBooleanColumnFilter::parseBoolean)
                    .collect(Collectors.toList());

            return expression.in(values);
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

}
