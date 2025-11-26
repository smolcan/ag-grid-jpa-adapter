package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.column;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.ColumnAdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.NumberFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.utils.Utils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;

public class NumberAdvancedFilterModel extends ColumnAdvancedFilterModel {
    
    private ScalarAdvancedFilterModelType type;
    private BigDecimal filter;
    private NumberFilterParams filterParams = NumberFilterParams.builder().build();
    
    public NumberAdvancedFilterModel(String colId) {
        super("number", colId);
    }
    
    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root) {
        Predicate predicate;

        // ensuring number compatibility
        // comparing any number types without problem, cast both to big decimal
        Expression<BigDecimal> path = Utils.getPath(root, this.getColId()).as(BigDecimal.class);
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


    public ScalarAdvancedFilterModelType getType() {
        return type;
    }

    public void setType(ScalarAdvancedFilterModelType type) {
        this.type = type;
    }

    public BigDecimal getFilter() {
        return filter;
    }

    public void setFilter(BigDecimal filter) {
        this.filter = filter;
    }

    public NumberFilterParams getFilterParams() {
        return filterParams;
    }

    public void setFilterParams(NumberFilterParams filterParams) {
        this.filterParams = filterParams;
    }
}
