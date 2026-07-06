package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.column;

import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.ColumnAdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.DateFilterParams;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DateAdvancedFilterModel<E, T> extends ColumnAdvancedFilterModel<E, T> {

    @Setter(onMethod_ = {@NonNull})
    private ScalarAdvancedFilterModelType type;
    private LocalDate filter;
    @NonNull
    private DateFilterParams filterParams = DateFilterParams.builder().build();
    
    public DateAdvancedFilterModel(@NonNull FieldPath<E, T> columnField) {
        super("date", columnField);
    }
    
    @Override
    @NonNull
    public Predicate toPredicate(@NonNull CriteriaBuilder cb, @NonNull Root<E> root) {
        this.filterParams.validateDate(this.filter);
        Predicate predicate;
        
        Expression<LocalDate> path = this.getColumnField().getPath(root).as(LocalDate.class);
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
                predicate = cb.lessThan(path, this.filter);
                if (filterParams.isIncludeBlanksInLessThan()) {
                    predicate = cb.or(predicate, cb.isNull(path));
                }
                break;
            }
            case lessThanOrEqual: {
                predicate = cb.lessThanOrEqualTo(path, this.filter);
                if (filterParams.isIncludeBlanksInLessThan()) {
                    predicate = cb.or(predicate, cb.isNull(path));
                }
                break;
            }
            case greaterThan: {
                predicate = cb.greaterThan(path, this.filter);
                if (filterParams.isIncludeBlanksInGreaterThan()) {
                    predicate = cb.or(predicate, cb.isNull(path));
                }
                break;
            }
            case greaterThanOrEqual: {
                predicate = cb.greaterThanOrEqualTo(path, this.filter);
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
