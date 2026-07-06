package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.column;

import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.ColumnAdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.NumberFilterParams;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class NumberAdvancedFilterModel<E, T extends Number> extends ColumnAdvancedFilterModel<E, T> {

    @Setter(onMethod_ = {@NonNull})
    private ScalarAdvancedFilterModelType type;
    private BigDecimal filter;
    @NonNull
    private NumberFilterParams filterParams = NumberFilterParams.builder().build();
    
    public NumberAdvancedFilterModel(@NonNull FieldPath<E, T> columnField) {
        super("number", columnField);
    }
    
    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<E> root) {
        Predicate predicate;

        // ensuring number compatibility
        // comparing any number types without problem, cast both to big decimal
        Expression<? extends Number> path = this.getColumnField().getPath(root);
        switch (this.type) {
            case blank: {
                predicate = cb.isNull(path);
                break;
            }
            case notBlank: {
                predicate = cb.isNotNull(path);
                break;
            }
            case equals: {
                predicate = cb.equal(path, this.filter);
                if (filterParams.isIncludeBlanksInEquals()) {
                    predicate = cb.or(predicate, cb.isNull(path));
                }
                break;
            }
            case notEqual: {
                predicate = cb.notEqual(path, this.filter);
                if (filterParams.isIncludeBlanksInNotEqual()) {
                    predicate = cb.or(predicate, cb.isNull(path));
                }
                break;
            }
            case lessThan: {
                predicate = cb.lt(path, this.filter);
                if (filterParams.isIncludeBlanksInLessThan()) {
                    predicate = cb.or(predicate, cb.isNull(path));
                }
                break;
            }
            case lessThanOrEqual: {
                predicate = cb.le(path, this.filter);
                if (filterParams.isIncludeBlanksInLessThan()) {
                    predicate = cb.or(predicate, cb.isNull(path));
                }
                break;
            }
            case greaterThan: {
                predicate = cb.gt(path, this.filter);
                if (filterParams.isIncludeBlanksInGreaterThan()) {
                    predicate = cb.or(predicate, cb.isNull(path));
                }
                break;
            }
            case greaterThanOrEqual: {
                predicate = cb.ge(path, this.filter);
                if (filterParams.isIncludeBlanksInGreaterThan()) {
                    predicate = cb.or(predicate, cb.isNull(path));
                }
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected value: " + this.type);
            }
        }

        return predicate;
    }
}
