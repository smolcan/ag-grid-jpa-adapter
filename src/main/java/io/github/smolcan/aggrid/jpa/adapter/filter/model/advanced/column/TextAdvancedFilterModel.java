package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.column;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.ColumnAdvancedFilterModel;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class TextAdvancedFilterModel extends ColumnAdvancedFilterModel {
    
    private TextAdvancedFilterModelType type;
    private String filter;
    
    public TextAdvancedFilterModel(String colId) {
        super("text", colId);
    }
    
    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root) {
        Predicate predicate;

        Path<String> path = root.get(this.getColId());
        switch (this.type) {
            case blank: {
                predicate = cb.or(cb.isNull(path), cb.equal(path, ""));
                break;
            }
            case notBlank: {
                predicate = cb.and(cb.isNotNull(path), cb.notEqual(path, ""));
                break;
            }
            case equals: {
                predicate = cb.equal(path, this.filter);
                break;
            }
            case notEqual: {
                predicate = cb.notEqual(path, filter);
                break;
            }
            case contains: {
                predicate = cb.like(path, "%" + filter + "%");
                break;
            }
            case notContains: {
                predicate = cb.notLike(path, "%" + filter + "%");
                break;
            }
            case startsWith: {
                predicate = cb.like(path, filter + "%");
                break;
            }
            case endsWith: {
                predicate = cb.like(path, "%" + filter);
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected value: " + this.type);
            }
        }
        
        return predicate;
    }

    public TextAdvancedFilterModelType getType() {
        return type;
    }

    public void setType(TextAdvancedFilterModelType type) {
        this.type = type;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

}
