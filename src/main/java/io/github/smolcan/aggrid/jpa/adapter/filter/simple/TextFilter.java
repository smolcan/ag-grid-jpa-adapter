package io.github.smolcan.aggrid.jpa.adapter.filter.simple;

import jakarta.persistence.criteria.*;

import java.util.Objects;
import java.util.function.Function;

public class TextFilter extends ColumnFilter {
    
    private SimpleFilterModelType type;
    private String filter;
    private String filterTo;
    private TextFilterParams filterParams = TextFilterParams.builder().build();
    
    public TextFilter() {
        super("text");
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String columnName) {
        Path<String> path = root.get(columnName);
        return this.toPredicate(cb, path);
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression) {
        Predicate predicate;
        
        boolean hasTextFormatter = this.filterParams.getTextFormatter() != null;
        boolean isCaseSensitive = this.filterParams.isCaseSensitive();
        String filter = this.filter != null && this.filterParams.isTrimInput() && !this.filter.equals(" ") 
                ? this.filter.trim()
                : this.filter;
        
        Expression<String> stringExpression = expression.as(String.class);
        Expression<String> textFormatterExpression = hasTextFormatter ? this.filterParams.getTextFormatter().apply(stringExpression) : null;
        Expression<String> lowercaseExpression = cb.lower(stringExpression);
        switch (this.type) {
            case empty: case blank: {
                if (hasTextFormatter) {
                    Predicate isNullPredicate = cb.isNull(textFormatterExpression);
                    Predicate isEmptyPredicate = cb.equal(textFormatterExpression, this.filterParams.getTextFormatter().apply(cb.literal("")));
                    predicate = cb.or(isNullPredicate, isEmptyPredicate);
                } else {
                    predicate = cb.or(cb.isNull(stringExpression), cb.equal(stringExpression, ""));
                }
                break;
            }
            case notBlank: {
                if (hasTextFormatter) {
                    Predicate isNotNullPredicate = cb.isNotNull(textFormatterExpression);
                    Predicate isNotEmptyPredicate = cb.notEqual(textFormatterExpression, this.filterParams.getTextFormatter().apply(cb.literal("")));
                    predicate = cb.or(isNotNullPredicate, isNotEmptyPredicate);
                } else {
                    predicate = cb.and(cb.isNotNull(stringExpression), cb.notEqual(stringExpression, ""));
                }
                break;
            }
            case equals: {
                if (hasTextFormatter) {
                    predicate = cb.equal(textFormatterExpression, this.filterParams.getTextFormatter().apply(cb.literal(filter)));
                } else if (!isCaseSensitive) {
                    predicate = cb.equal(lowercaseExpression, cb.lower(cb.literal(filter)));
                } else {
                    predicate = cb.equal(stringExpression, filter);
                }
                break;
            }
            case notEqual: {
                if (hasTextFormatter) {
                    predicate = cb.notEqual(textFormatterExpression, this.filterParams.getTextFormatter().apply(cb.literal(filter)));
                } else if (!isCaseSensitive) {
                    predicate = cb.notEqual(lowercaseExpression, cb.lower(cb.literal(filter)));
                } else {
                    predicate = cb.notEqual(stringExpression, filter);
                }
                break;
            }
            case contains: {
                if (hasTextFormatter) {
                    Expression<String> likeExpression = cb.concat(cb.concat("%", this.filterParams.getTextFormatter().apply(cb.literal(filter))), "%");
                    predicate = cb.like(textFormatterExpression, likeExpression);
                } else if (!isCaseSensitive) {
                    Expression<String> likeExpression = cb.concat(cb.concat("%", cb.lower(cb.literal(filter))), "%");
                    predicate = cb.like(lowercaseExpression, likeExpression);
                } else {
                    predicate = cb.like(stringExpression, "%" + filter + "%");
                }
                break;
            }
            case notContains: {
                if (hasTextFormatter) {
                    Expression<String> likeExpression = cb.concat(cb.concat("%", this.filterParams.getTextFormatter().apply(cb.literal(filter))), "%");
                    predicate = cb.notLike(textFormatterExpression, likeExpression);
                } else if (!isCaseSensitive) {
                    Expression<String> likeExpression = cb.concat(cb.concat("%", cb.lower(cb.literal(filter))), "%");
                    predicate = cb.notLike(lowercaseExpression, likeExpression);
                } else {
                    predicate = cb.notLike(stringExpression, "%" + filter + "%");
                }
                break;
            }
            case startsWith: {
                if (hasTextFormatter) {
                    Expression<String> likeExpression = cb.concat(this.filterParams.getTextFormatter().apply(cb.literal(filter)), "%");
                    predicate = cb.like(textFormatterExpression, likeExpression);
                } else if (!isCaseSensitive) {
                    Expression<String> likeExpression = cb.concat(cb.lower(cb.literal(filter)), "%");
                    predicate = cb.like(lowercaseExpression, likeExpression);
                } else {
                    predicate = cb.like(stringExpression, filter + "%");
                }
                break;
            }
            case endsWith: {
                if (hasTextFormatter) {
                    Expression<String> likeExpression = cb.concat("%", this.filterParams.getTextFormatter().apply(cb.literal(filter)));
                    predicate = cb.like(textFormatterExpression, likeExpression);
                } else if (!isCaseSensitive) {
                    Expression<String> likeExpression = cb.concat("%", cb.lower(cb.literal(filter)));
                    predicate = cb.like(lowercaseExpression, likeExpression);
                } else {
                    predicate = cb.like(stringExpression, "%" + filter);
                }
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected value: " + this.type);
            }
        }

        return predicate;
    }

    public SimpleFilterModelType getType() {
        return type;
    }

    public void setType(SimpleFilterModelType type) {
        this.type = type;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getFilterTo() {
        return filterTo;
    }

    public void setFilterTo(String filterTo) {
        this.filterTo = filterTo;
    }

    public TextFilterParams getFilterParams() {
        return filterParams;
    }

    public void setFilterParams(TextFilterParams filterParams) {
        this.filterParams = Objects.requireNonNull(filterParams);
    }



    public static class TextFilterParams {

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
            this.textFormatter = Objects.requireNonNull(textFormatter);
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

}
