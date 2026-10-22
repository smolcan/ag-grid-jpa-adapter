package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.column;

import io.github.smolcan.aggrid.jpa.adapter.column.ColumnSource;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.ColumnAdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.TextFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.TextMatcherParams;
import io.github.smolcan.aggrid.jpa.adapter.utils.Utils;
import jakarta.persistence.criteria.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class TextAdvancedFilterModel<E> extends ColumnAdvancedFilterModel<E, String> {

    @Setter(onMethod_ = {@NonNull})
    private TextAdvancedFilterModelType type;
    private String filter;
    @NonNull
    private TextFilterParams filterParams = TextFilterParams.builder().build();
    
    public TextAdvancedFilterModel(@NonNull ColumnSource<? super E, String> columnField) {
        super("text", columnField);
    }
    
    @Override
    @NonNull
    public Predicate toPredicate(@NonNull CriteriaBuilder cb, @NonNull Root<? extends E> root) {

        Expression<String> filterExpression = this.filterParams.generateExpressionFromFilterParams(cb, this.filter != null ? cb.literal(this.filter) : cb.nullLiteral(String.class));
        Expression<String> valueExpression = this.filterParams.generateExpressionFromFilterParams(cb, this.getColumnField().getExpression(cb, root));

        // check if provided custom text matcher
        if (this.filterParams.getTextMatcher() != null) {
            var textMatcherParams = TextMatcherParams.builder()
                    .filterOption(SimpleFilterModelType.valueOf(this.type.name()))
                    .value(valueExpression)
                    .filterText(filterExpression)
                    .build();

            return this.filterParams.getTextMatcher().apply(cb, textMatcherParams);
        }

        // filter text for the LIKE patterns, with its wildcards escaped so that they match literally
        Expression<String> likeFilterExpression = this.filterParams.generateExpressionFromFilterParams(cb, this.filter != null ? cb.literal(Utils.escapeLike(this.filter)) : cb.nullLiteral(String.class));
        boolean escapeLike = this.filter != null && Utils.needsLikeEscape(this.filter);

        Predicate predicate;
        switch (this.type) {
            case blank: {
                predicate = cb.or(cb.isNull(valueExpression), cb.equal(cb.length(valueExpression), 0));
                break;
            }
            case notBlank: {
                predicate = cb.and(cb.isNotNull(valueExpression), cb.greaterThan(cb.length(valueExpression), 0));
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
                predicate = Utils.like(cb, valueExpression, cb.concat(cb.concat("%", likeFilterExpression), "%"), escapeLike);
                break;
            }
            case notContains: {
                predicate = Utils.notLike(cb, valueExpression, cb.concat(cb.concat("%", likeFilterExpression), "%"), escapeLike);
                break;
            }
            case startsWith: {
                predicate = Utils.like(cb, valueExpression, cb.concat(likeFilterExpression, "%"), escapeLike);
                break;
            }
            case endsWith: {
                predicate = Utils.like(cb, valueExpression, cb.concat("%", likeFilterExpression), escapeLike);
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected value: " + this.type);
            }
        }
        
        return predicate;
    }
}
