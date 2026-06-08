package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.column;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.ColumnAdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.TextFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.TextMatcherParams;
import io.github.smolcan.aggrid.jpa.adapter.utils.Utils;
import jakarta.persistence.criteria.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextAdvancedFilterModel extends ColumnAdvancedFilterModel {
    
    private TextAdvancedFilterModelType type;
    private String filter;
    private TextFilterParams filterParams = TextFilterParams.builder().build();
    
    public TextAdvancedFilterModel(String colId) {
        super("text", colId);
    }
    
    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root) {

        Expression<String> filterExpression = this.filterParams.generateExpressionFromFilterParams(cb, cb.literal(this.filter));
        Expression<String> valueExpression = this.filterParams.generateExpressionFromFilterParams(cb, Utils.getPath(root, this.getColId()).as(String.class));

        // check if provided custom text matcher
        if (this.filterParams.getTextMatcher() != null) {
            var textMatcherParams = TextMatcherParams.builder()
                    .filterOption(SimpleFilterModelType.valueOf(this.type.name()))
                    .value(valueExpression)
                    .filterText(filterExpression)
                    .build();

            return this.filterParams.getTextMatcher().apply(cb, textMatcherParams);
        }
        
        Predicate predicate;
        switch (this.type) {
            case blank: {
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
                throw new IllegalStateException("Unexpected value: " + this.type);
            }
        }
        
        return predicate;
    }
}
