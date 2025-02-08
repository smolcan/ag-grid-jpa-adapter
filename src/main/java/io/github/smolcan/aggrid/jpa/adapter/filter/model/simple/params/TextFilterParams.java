package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

import jakarta.persistence.criteria.Expression;

import java.util.function.Function;

public class TextFilterParams implements ISimpleFilterParams {

    // By default, text filtering is case-insensitive. Set this to true to make text filtering case-sensitive.
    private final boolean caseSensitive;
    // Formats the text before applying the filter compare logic. 
    // Useful if you want to substitute accented characters, for example.
    private final Function<Expression<String>, Expression<String>> textFormatter;
    // If true, the input that the user enters will be trimmed when the filter is applied, so any leading or trailing whitespace will be removed. 
    // If only whitespace is entered, it will be left as-is. 
    private final boolean trimInput;

    private TextFilterParams(boolean caseSensitive, Function<Expression<String>, Expression<String>> textFormatter, boolean trimInput) {
        this.caseSensitive = caseSensitive;
        this.textFormatter = textFormatter;
        this.trimInput = trimInput;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public Function<Expression<String>, Expression<String>> getTextFormatter() {
        return textFormatter;
    }

    public boolean isTrimInput() {
        return trimInput;
    }

    public static class Builder {
        private boolean caseSensitive = false;
        private Function<Expression<String>, Expression<String>> textFormatter;
        private boolean trimInput = false;

        public Builder caseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }

        public Builder textFormatter(Function<Expression<String>, Expression<String>> textFormatter) {
            this.textFormatter = textFormatter;
            return this;
        }

        public Builder trimInput(boolean trimInput) {
            this.trimInput = trimInput;
            return this;
        }

        public TextFilterParams build() {
            return new TextFilterParams(this.caseSensitive, this.textFormatter, this.trimInput);
        }
    }

}