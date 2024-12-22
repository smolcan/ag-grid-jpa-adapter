package io.github.smolcan.aggrid.jpa.adapter.filter.advanced.column;

import io.github.smolcan.aggrid.jpa.adapter.filter.advanced.ColumnAdvancedFilterModel;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class BooleanAdvancedFilterModel extends ColumnAdvancedFilterModel {
    private BooleanAdvancedFilterModelType type;

    public BooleanAdvancedFilterModel(String colId) {
        super("boolean", colId);
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root) {
        Predicate predicate;

        Path<Boolean> path = root.get(this.getColId());
        switch (this.type) {
            case TRUE: {
                predicate = cb.isTrue(path);
                break;
            }
            case FALSE: {
                predicate = cb.isFalse(path);
                break;
            }
            case blank: {
                predicate = cb.isNull(path);
                break;
            }
            case notBlank: {
                predicate = cb.isNotNull(path);
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected value: " + this.type);
            }
        }
        
        return predicate;
    }

    public BooleanAdvancedFilterModelType getType() {
        return type;
    }

    public void setType(BooleanAdvancedFilterModelType type) {
        this.type = type;
    }
}
