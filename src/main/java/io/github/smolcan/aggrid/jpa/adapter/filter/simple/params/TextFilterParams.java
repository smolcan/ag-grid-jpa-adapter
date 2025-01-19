package io.github.smolcan.aggrid.jpa.adapter.filter.simple.params;

import jakarta.persistence.criteria.Expression;

import java.util.function.Function;

public class TextFilterParams extends SimpleFilterParams {

    // By default, text filtering is case-insensitive. Set this to true to make text filtering case-sensitive.
    private boolean caseSensitive = false;
    // Formats the text before applying the filter compare logic. 
    // Useful if you want to substitute accented characters, for example.
    private Function<Expression<String>, Expression<String>> textFormatter;


    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public Function<Expression<String>, Expression<String>> getTextFormatter() {
        return textFormatter;
    }

    public void setTextFormatter(Function<Expression<String>, Expression<String>> textFormatter) {
        this.textFormatter = textFormatter;
    }
}
