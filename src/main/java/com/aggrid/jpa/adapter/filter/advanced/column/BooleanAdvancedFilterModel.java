package com.aggrid.jpa.adapter.filter.advanced.column;

import com.aggrid.jpa.adapter.filter.advanced.ColumnAdvancedFilterModel;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class BooleanAdvancedFilterModel extends ColumnAdvancedFilterModel {
    private boolean type;

    public BooleanAdvancedFilterModel(String colId) {
        super("boolean", colId);
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root) {
        Predicate predicate;

        Path<Boolean> path = root.get(this.getColId());
        if (this.type) {
            predicate = cb.isTrue(path);
        } else {
            predicate = cb.isFalse(path);
        }
        
        return predicate;
    }

    public boolean isType() {
        return type;
    }

    public void setType(boolean type) {
        this.type = type;
    }
}
