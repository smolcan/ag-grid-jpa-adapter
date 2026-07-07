package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Tolerate;

import java.util.Set;
import java.util.function.BiFunction;

@Getter
@Builder(builderClassName = "Builder")
public class TextFilterParams implements ISimpleFilterParams {
    /**
     * @param textMatcher overrides how filtering is done for the user input, returning the predicate directly.
     * @return the custom text matcher.
     */
    private final BiFunction<CriteriaBuilder, TextMatcherParams, Predicate> textMatcher;
    /**
     * @param caseSensitive set {@code true} to make text filtering case-sensitive (default is case-insensitive).
     * @return whether text filtering is case-sensitive.
     */
    private final boolean caseSensitive;
    /**
     * @param textFormatter formats the text before comparing (e.g. to substitute accented characters).
     * @return the text formatter.
     */
    private final BiFunction<CriteriaBuilder, Expression<String>, Expression<String>> textFormatter;
    /**
     * @param trimInput set {@code true} to trim leading/trailing whitespace from the input before filtering.
     * @return whether the input is trimmed.
     */
    private final boolean trimInput;
    /**
     * @param filterOptions the filter options allowed on this column.
     * @return the allowed filter options.
     */
    @NonNull
    private final Set<SimpleFilterModelType> filterOptions;

    /**
     * With given expression, generate new expression according to filter params
     *
     * @param cb            criteria builder
     * @param expression    expression
     * @return              new expression generated from filter params
     */
    @NonNull
    public Expression<String> generateExpressionFromFilterParams(@NonNull CriteriaBuilder cb, @NonNull Expression<String> expression) {
        if (this.trimInput) {
            expression = cb.trim(expression);
        }

        if (this.textFormatter != null) {
            // apply custom text formatter
            expression = this.textFormatter.apply(cb, expression);
        } else if (!this.caseSensitive) {
            // custom text formatter not present, apply case-insensitive
            expression = cb.lower(expression);
        }

        return expression;
    }
    
    public static class Builder {
        
        private Set<SimpleFilterModelType> filterOptions = Set.of(SimpleFilterModelType.values());

        @Tolerate
        @NonNull
        public Builder filterOptions(@NonNull SimpleFilterModelType ...values) {
            this.filterOptions = Set.of(values);
            return this;
        }
    }
}