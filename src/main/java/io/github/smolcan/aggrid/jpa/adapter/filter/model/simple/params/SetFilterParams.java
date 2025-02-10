package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

import java.util.function.BiFunction;

public class SetFilterParams implements IFilterParams {
    private final boolean caseSensitive;
    private final BiFunction<CriteriaBuilder, Expression<String>, Expression<String>> textFormatter;
    
    private SetFilterParams(Builder builder) {
        this.caseSensitive = builder.caseSensitive;
        this.textFormatter = builder.textFormatter;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public BiFunction<CriteriaBuilder, Expression<String>, Expression<String>> getTextFormatter() {
        return textFormatter;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        
        private boolean caseSensitive = false;
        /**
         * If specified, this formats the text before applying the compare logic, useful for
         * instance to substitute accented characters.
         */
        private BiFunction<CriteriaBuilder, Expression<String>, Expression<String>> textFormatter;
        
        public Builder caseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }
        
        public Builder textFormatter(BiFunction<CriteriaBuilder, Expression<String>, Expression<String>> textFormatter) {
            this.textFormatter = textFormatter;
            return this;
        }
        
        public SetFilterParams build() {
            return new SetFilterParams(this);
        }
    }
}
