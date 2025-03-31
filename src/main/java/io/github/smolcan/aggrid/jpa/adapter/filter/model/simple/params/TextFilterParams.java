package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.Set;
import java.util.function.BiFunction;

public class TextFilterParams implements ISimpleFilterParams {
    // Used to override how to filter based on the user input. Returns true if the value passes the filter, otherwise false.
    private final BiFunction<CriteriaBuilder, TextMatcherParams, Predicate> textMatcher;
    // By default, text filtering is case-insensitive. Set this to true to make text filtering case-sensitive.
    private final boolean caseSensitive;
    // Formats the text before applying the filter compare logic. 
    // Useful if you want to substitute accented characters, for example.
    private final BiFunction<CriteriaBuilder, Expression<String>, Expression<String>> textFormatter;
    // If true, the input that the user enters will be trimmed when the filter is applied, so any leading or trailing whitespace will be removed. 
    // If only whitespace is entered, it will be left as-is. 
    private final boolean trimInput;
    private final Set<SimpleFilterModelType> filterOptions;

    private TextFilterParams(Builder builder) {
        this.textMatcher = builder.textMatcher;
        this.caseSensitive = builder.caseSensitive;
        this.textFormatter = builder.textFormatter;
        this.trimInput = builder.trimInput;
        this.filterOptions = builder.filterOptions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public BiFunction<CriteriaBuilder, TextMatcherParams, Predicate> getTextMatcher() {
        return textMatcher;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public  BiFunction<CriteriaBuilder, Expression<String>, Expression<String>> getTextFormatter() {
        return textFormatter;
    }

    public boolean isTrimInput() {
        return trimInput;
    }

    public Set<SimpleFilterModelType> getFilterOptions() {
        return filterOptions;
    }

    /**
     * With given expression, generate new expression according to filter params
     *
     * @param cb            criteria builder
     * @param expression    expression
     * @return              new expression generated from filter params
     */
    public Expression<String> generateExpressionFromFilterParams(CriteriaBuilder cb, Expression<String> expression) {
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
        private BiFunction<CriteriaBuilder, TextMatcherParams, Predicate> textMatcher;
        private boolean caseSensitive = false;
        private BiFunction<CriteriaBuilder, Expression<String>, Expression<String>> textFormatter;
        private boolean trimInput = false;
        private Set<SimpleFilterModelType> filterOptions = Set.of(SimpleFilterModelType.values());

        public Builder textMatcher(BiFunction<CriteriaBuilder, TextMatcherParams, Predicate> textMatcher) {
            this.textMatcher = textMatcher;
            return this;
        }
        
        public Builder caseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }

        public Builder textFormatter( BiFunction<CriteriaBuilder, Expression<String>, Expression<String>> textFormatter) {
            this.textFormatter = textFormatter;
            return this;
        }

        public Builder trimInput(boolean trimInput) {
            this.trimInput = trimInput;
            return this;
        }
        
        public Builder filterOptions(SimpleFilterModelType ...values) {
            this.filterOptions = Set.of(values);
            return this;
        }

        public TextFilterParams build() {
            return new TextFilterParams(this);
        }
    }
    
    
    public static class TextMatcherParams {

        /**
         * The applicable filter option being tested.
         */
        private final SimpleFilterModelType filterOption;
        
        /**
         * The expression about to be filtered.
         * If a `textFormatter` is provided, this value will have been formatted.
         * If no `textFormatter` is provided and `caseSensitive` is not provided or is `false`,
         * the value will have been converted to lower case.
         */
        private final Expression<String> value;

        /**
         * The value to filter by.
         * If a `textFormatter` is provided, this value will have been formatted.
         * If no `textFormatter` is provided and `caseSensitive` is not provided or is `false`,
         * the value will have been converted to lower case.
         */
        private final Expression<String> filterText;
        
        
        private TextMatcherParams(Builder builder) {
            this.filterOption = builder.filterOption;
            this.value = builder.value;
            this.filterText = builder.filterText;
        }

        
        public SimpleFilterModelType getFilterOption() {
            return filterOption;
        }

        public Expression<String> getValue() {
            return value;
        }

        public Expression<String> getFilterText() {
            return filterText;
        }

        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private SimpleFilterModelType filterOption;
            private Expression<String> value;
            private Expression<String> filterText;

            public Builder filterOption(SimpleFilterModelType filterOption) {
                this.filterOption = filterOption;
                return this;
            }

            public Builder value(Expression<String> value) {
                this.value = value;
                return this;
            }

            public Builder filterText(Expression<String> filterText) {
                this.filterText = filterText;
                return this;
            }

            public TextMatcherParams build() {
                return new TextMatcherParams(this);
            }
        }
    }

}