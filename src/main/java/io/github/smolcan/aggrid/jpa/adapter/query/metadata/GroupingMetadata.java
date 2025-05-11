package io.github.smolcan.aggrid.jpa.adapter.query.metadata;


import jakarta.persistence.criteria.Expression;

import java.util.Objects;

/**
 * Metadata for a grouping expression, including the associated column name.
 */
public class GroupingMetadata {
    
    private final Expression<?> gropingExpression;
    private final String column;
    
    private GroupingMetadata(Builder builder) {
        this.gropingExpression = builder.gropingExpression;
        this.column = builder.column;
    }

    public Expression<?> getGropingExpression() {
        return gropingExpression;
    }

    public String getColumn() {
        return column;
    }

    public static Builder builder(Expression<?> expression) {
        return new Builder(expression);
    }

    public static class Builder {
        private Expression<?> gropingExpression;
        private String column;
        
        public Builder(Expression<?> expression) {
            Objects.requireNonNull(expression);
            this.gropingExpression = expression;
        }

        public Builder gropingExpression(Expression<?> gropingExpression) {
            Objects.requireNonNull(gropingExpression);
            this.gropingExpression = gropingExpression;
            return this;
        }

        public Builder column(String column) {
            this.column = column;
            return this;
        }

        public GroupingMetadata build() {
            return new GroupingMetadata(this);
        }
    }
}
