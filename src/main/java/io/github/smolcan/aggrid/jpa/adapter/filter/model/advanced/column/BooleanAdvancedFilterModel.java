package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.column;

import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.ColumnAdvancedFilterModel;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter(onMethod_ = {@NonNull})
public class BooleanAdvancedFilterModel<E> extends ColumnAdvancedFilterModel<E, Boolean> {
    private BooleanAdvancedFilterModelType type;

    public BooleanAdvancedFilterModel(@NonNull FieldPath<E, Boolean> columnField) {
        super("boolean", columnField);
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<E> root) {
        Predicate predicate;

        Path<Boolean> path = this.getColumnField().getPath(root);
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
