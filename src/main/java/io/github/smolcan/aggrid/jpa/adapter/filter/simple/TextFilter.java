package io.github.smolcan.aggrid.jpa.adapter.filter.simple;

import jakarta.persistence.criteria.*;

public class TextFilter extends ColumnFilter {
    
    private SimpleFilterModelType type;
    private String filter;
    private String filterTo;
    
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

        Expression<String> stringExpression = expression.as(String.class);
        switch (this.type) {
            case empty: case blank: {
                predicate = cb.or(cb.isNull(stringExpression), cb.equal(stringExpression, ""));
                break;
            }
            case notBlank: {
                predicate = cb.and(cb.isNotNull(stringExpression), cb.notEqual(stringExpression, ""));
                break;
            }
            case equals: {
                predicate = cb.equal(stringExpression, filter);
                break;
            }
            case notEqual: {
                predicate = cb.notEqual(stringExpression, filter);
                break;
            }
            case contains: {
                predicate = cb.like(stringExpression, "%" + filter + "%");
                break;
            }
            case notContains: {
                predicate = cb.notLike(stringExpression, "%" + filter + "%");
                break;
            }
            case startsWith: {
                predicate = cb.like(stringExpression, filter + "%");
                break;
            }
            case endsWith: {
                predicate = cb.like(stringExpression, "%" + filter);
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
}
