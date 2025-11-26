package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.column;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.ColumnAdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.DateFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.utils.Utils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalDate;

public class DateAdvancedFilterModel extends ColumnAdvancedFilterModel {
    
    private ScalarAdvancedFilterModelType type;
    private LocalDate filter;
    private DateFilterParams filterParams = DateFilterParams.builder().build();
    
    public DateAdvancedFilterModel(String colId) {
        super("date", colId);
    }
    
    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root) {
        this.filterParams.validateDate(this.filter);
        Predicate predicate;

        Expression<LocalDate> path = Utils.getPath(root, this.getColId()).as(LocalDate.class);
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

    public ScalarAdvancedFilterModelType getType() {
        return type;
    }

    public void setType(ScalarAdvancedFilterModelType type) {
        this.type = type;
    }

    public LocalDate getFilter() {
        return filter;
    }

    public void setFilter(LocalDate filter) {
        this.filter = filter;
    }

    public DateFilterParams getFilterParams() {
        return filterParams;
    }

    public void setFilterParams(DateFilterParams filterParams) {
        this.filterParams = filterParams;
    }
}
