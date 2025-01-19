package io.github.smolcan.aggrid.jpa.adapter.filter.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.simple.params.TextFilterParams;
import jakarta.persistence.criteria.*;

import java.util.Objects;

public class TextFilter extends ColumnFilter {
    
    private SimpleFilterModelType type;
    private String filter;
    private String filterTo;
    private TextFilterParams filterParams = new TextFilterParams();
    
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
                    predicate = cb.equal(textFormatterExpression, this.filterParams.getTextFormatter().apply(cb.literal(this.filter)));
                } else if (!isCaseSensitive) {
                    predicate = cb.equal(lowercaseExpression, cb.lower(cb.literal(this.filter)));
                } else {
                    predicate = cb.equal(stringExpression, this.filter);
                }
                break;
            }
            case notEqual: {
                if (hasTextFormatter) {
                    predicate = cb.notEqual(textFormatterExpression, this.filterParams.getTextFormatter().apply(cb.literal(this.filter)));
                } else if (!isCaseSensitive) {
                    predicate = cb.notEqual(lowercaseExpression, cb.lower(cb.literal(this.filter)));
                } else {
                    predicate = cb.notEqual(stringExpression, this.filter);
                }
                break;
            }
            case contains: {
                if (hasTextFormatter) {
                    Expression<String> likeExpression = cb.concat(cb.concat("%", this.filterParams.getTextFormatter().apply(cb.literal(this.filter))), "%");
                    predicate = cb.like(textFormatterExpression, likeExpression);
                } else if (!isCaseSensitive) {
                    Expression<String> likeExpression = cb.concat(cb.concat("%", cb.lower(cb.literal(this.filter))), "%");
                    predicate = cb.like(lowercaseExpression, likeExpression);
                } else {
                    predicate = cb.like(stringExpression, "%" + this.filter + "%");
                }
                break;
            }
            case notContains: {
                if (hasTextFormatter) {
                    Expression<String> likeExpression = cb.concat(cb.concat("%", this.filterParams.getTextFormatter().apply(cb.literal(this.filter))), "%");
                    predicate = cb.notLike(textFormatterExpression, likeExpression);
                } else if (!isCaseSensitive) {
                    Expression<String> likeExpression = cb.concat(cb.concat("%", cb.lower(cb.literal(this.filter))), "%");
                    predicate = cb.notLike(lowercaseExpression, likeExpression);
                } else {
                    predicate = cb.notLike(stringExpression, "%" + this.filter + "%");
                }
                break;
            }
            case startsWith: {
                if (hasTextFormatter) {
                    Expression<String> likeExpression = cb.concat(this.filterParams.getTextFormatter().apply(cb.literal(this.filter)), "%");
                    predicate = cb.like(textFormatterExpression, likeExpression);
                } else if (!isCaseSensitive) {
                    Expression<String> likeExpression = cb.concat(cb.lower(cb.literal(this.filter)), "%");
                    predicate = cb.like(lowercaseExpression, likeExpression);
                } else {
                    predicate = cb.like(stringExpression, this.filter + "%");
                }
                break;
            }
            case endsWith: {
                if (hasTextFormatter) {
                    Expression<String> likeExpression = cb.concat("%", this.filterParams.getTextFormatter().apply(cb.literal(this.filter)));
                    predicate = cb.like(textFormatterExpression, likeExpression);
                } else if (!isCaseSensitive) {
                    Expression<String> likeExpression = cb.concat("%", cb.lower(cb.literal(this.filter)));
                    predicate = cb.like(lowercaseExpression, likeExpression);
                } else {
                    predicate = cb.like(stringExpression, "%" + this.filter);
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
}
