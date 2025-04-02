package io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.TextFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.TextFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.TextMatcherParams;
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
    public TextFilterParams getDefaultFilterParams() {
        return TextFilterParams.builder().build();
    }

    @Override
    protected Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression, TextFilterModel filterModel) {
        if (!this.filterParams.getFilterOptions().contains(filterModel.getType())) {
            throw new IllegalArgumentException("Filter type " + filterModel.getType() + " not allowed for this column");
        }
        
        Expression<String> filterExpression = this.filterParams.generateExpressionFromFilterParams(cb, cb.literal(filterModel.getFilter()));
        Expression<String> valueExpression = this.filterParams.generateExpressionFromFilterParams(cb, expression.as(String.class));
        
        // check if provided custom text matcher
        if (this.filterParams.getTextMatcher() != null) {
            var textMatcherParams = TextMatcherParams.builder()
                    .filterOption(filterModel.getType())
                    .value(valueExpression)
                    .filterText(filterExpression)
                    .build();
            
            return this.filterParams.getTextMatcher().apply(cb, textMatcherParams);
        }
        
        
        Predicate predicate;
        switch (filterModel.getType()) {
            case empty: case blank: {
                predicate = cb.or(cb.isNull(valueExpression), cb.equal(valueExpression, ""));
                break;
            }
            case notBlank: {
                predicate = cb.and(cb.isNotNull(valueExpression), cb.notEqual(valueExpression, ""));
                break;
            }
            case equals: {
                predicate = cb.equal(valueExpression, filterExpression);
                break;
            }
            case notEqual: {
                predicate = cb.notEqual(valueExpression, filterExpression);
                break;
            }
            case contains: {
                predicate = cb.like(valueExpression, cb.concat(cb.concat("%", filterExpression), "%"));
                break;
            }
            case notContains: {
                predicate = cb.notLike(valueExpression, cb.concat(cb.concat("%", filterExpression), "%"));
                break;
            }
            case startsWith: {
                predicate = cb.like(valueExpression, cb.concat(filterExpression, "%"));
                break;
            }
            case endsWith: {
                predicate = cb.like(valueExpression, cb.concat("%", filterExpression));
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected value: " + filterModel.getType());
            }
        }

        return predicate;
    }
}
