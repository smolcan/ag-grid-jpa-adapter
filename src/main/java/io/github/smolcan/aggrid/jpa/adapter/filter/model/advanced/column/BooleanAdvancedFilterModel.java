package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.column;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.ColumnAdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.utils.Utils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BooleanAdvancedFilterModel extends ColumnAdvancedFilterModel {
    private BooleanAdvancedFilterModelType type;

    public BooleanAdvancedFilterModel(String colId) {
        super("boolean", colId);
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root) {
        Predicate predicate;

        @SuppressWarnings("unchecked")
        Path<Boolean> path = (Path<Boolean>) Utils.getPath(root, this.getColId());
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
}
