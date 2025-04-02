package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import jakarta.persistence.criteria.Expression;

public class TextMatcherParams {

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


    private TextMatcherParams(TextMatcherParams.Builder builder) {
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

    public static TextMatcherParams.Builder builder() {
        return new TextMatcherParams.Builder();
    }

    public static class Builder {
        private SimpleFilterModelType filterOption;
        private Expression<String> value;
        private Expression<String> filterText;

        public TextMatcherParams.Builder filterOption(SimpleFilterModelType filterOption) {
            this.filterOption = filterOption;
            return this;
        }

        public TextMatcherParams.Builder value(Expression<String> value) {
            this.value = value;
            return this;
        }

        public TextMatcherParams.Builder filterText(Expression<String> filterText) {
            this.filterText = filterText;
            return this;
        }

        public TextMatcherParams build() {
            return new TextMatcherParams(this);
        }
    }
}