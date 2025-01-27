package io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.TextFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.TextFilterParams;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.Map;
import java.util.Optional;

public class AgTextColumnFilter extends SimpleFilter<TextFilterModel, TextFilterParams> {
    
    @Override
    public TextFilterModel recognizeFilterModel(Map<String, Object> filterModel) {
        TextFilterModel textFilter = new TextFilterModel();
        textFilter.setType(SimpleFilterModelType.valueOf(filterModel.get("type").toString()));
        textFilter.setFilter(Optional.ofNullable(filterModel.get("filter")).map(Object::toString).orElse(null));
        textFilter.setFilterTo(Optional.ofNullable(filterModel.get("filterTo")).map(Object::toString).orElse(null));
        return textFilter;
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression, TextFilterModel filterModel, TextFilterParams filterParams) {
        Predicate predicate;

        boolean hasTextFormatter = filterParams.getTextFormatter() != null;
        boolean isCaseSensitive = filterParams.isCaseSensitive();
        String filter = filterModel.getFilter() != null && filterParams.isTrimInput() && !filterModel.getFilter().equals(" ")
                ? filterModel.getFilter().trim()
                : filterModel.getFilter();

        Expression<String> stringExpression = expression.as(String.class);
        Expression<String> textFormatterExpression = hasTextFormatter ? filterParams.getTextFormatter().apply(stringExpression) : null;
        Expression<String> lowercaseExpression = cb.lower(stringExpression);
        switch (filterModel.getType()) {
            case empty: case blank: {
                if (hasTextFormatter) {
                    Predicate isNullPredicate = cb.isNull(textFormatterExpression);
                    Predicate isEmptyPredicate = cb.equal(textFormatterExpression, filterParams.getTextFormatter().apply(cb.literal("")));
                    predicate = cb.or(isNullPredicate, isEmptyPredicate);
                } else {
                    predicate = cb.or(cb.isNull(stringExpression), cb.equal(stringExpression, ""));
                }
                break;
            }
            case notBlank: {
                if (hasTextFormatter) {
                    Predicate isNotNullPredicate = cb.isNotNull(textFormatterExpression);
                    Predicate isNotEmptyPredicate = cb.notEqual(textFormatterExpression, filterParams.getTextFormatter().apply(cb.literal("")));
                    predicate = cb.or(isNotNullPredicate, isNotEmptyPredicate);
                } else {
                    predicate = cb.and(cb.isNotNull(stringExpression), cb.notEqual(stringExpression, ""));
                }
                break;
            }
            case equals: {
                if (hasTextFormatter) {
                    predicate = cb.equal(textFormatterExpression, filterParams.getTextFormatter().apply(cb.literal(filter)));
                } else if (!isCaseSensitive) {
                    predicate = cb.equal(lowercaseExpression, cb.lower(cb.literal(filter)));
                } else {
                    predicate = cb.equal(stringExpression, filter);
                }
                break;
            }
            case notEqual: {
                if (hasTextFormatter) {
                    predicate = cb.notEqual(textFormatterExpression, filterParams.getTextFormatter().apply(cb.literal(filter)));
                } else if (!isCaseSensitive) {
                    predicate = cb.notEqual(lowercaseExpression, cb.lower(cb.literal(filter)));
                } else {
                    predicate = cb.notEqual(stringExpression, filter);
                }
                break;
            }
            case contains: {
                if (hasTextFormatter) {
                    Expression<String> likeExpression = cb.concat(cb.concat("%", filterParams.getTextFormatter().apply(cb.literal(filter))), "%");
                    predicate = cb.like(textFormatterExpression, likeExpression);
                } else if (!isCaseSensitive) {
                    Expression<String> likeExpression = cb.concat(cb.concat("%", cb.lower(cb.literal(filter))), "%");
                    predicate = cb.like(lowercaseExpression, likeExpression);
                } else {
                    predicate = cb.like(stringExpression, "%" + filter + "%");
                }
                break;
            }
            case notContains: {
                if (hasTextFormatter) {
                    Expression<String> likeExpression = cb.concat(cb.concat("%", filterParams.getTextFormatter().apply(cb.literal(filter))), "%");
                    predicate = cb.notLike(textFormatterExpression, likeExpression);
                } else if (!isCaseSensitive) {
                    Expression<String> likeExpression = cb.concat(cb.concat("%", cb.lower(cb.literal(filter))), "%");
                    predicate = cb.notLike(lowercaseExpression, likeExpression);
                } else {
                    predicate = cb.notLike(stringExpression, "%" + filter + "%");
                }
                break;
            }
            case startsWith: {
                if (hasTextFormatter) {
                    Expression<String> likeExpression = cb.concat(filterParams.getTextFormatter().apply(cb.literal(filter)), "%");
                    predicate = cb.like(textFormatterExpression, likeExpression);
                } else if (!isCaseSensitive) {
                    Expression<String> likeExpression = cb.concat(cb.lower(cb.literal(filter)), "%");
                    predicate = cb.like(lowercaseExpression, likeExpression);
                } else {
                    predicate = cb.like(stringExpression, filter + "%");
                }
                break;
            }
            case endsWith: {
                if (hasTextFormatter) {
                    Expression<String> likeExpression = cb.concat("%", filterParams.getTextFormatter().apply(cb.literal(filter)));
                    predicate = cb.like(textFormatterExpression, likeExpression);
                } else if (!isCaseSensitive) {
                    Expression<String> likeExpression = cb.concat("%", cb.lower(cb.literal(filter)));
                    predicate = cb.like(lowercaseExpression, likeExpression);
                } else {
                    predicate = cb.like(stringExpression, "%" + filter);
                }
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected value: " + filterModel.getType());
            }
        }

        return predicate;
    }
}
