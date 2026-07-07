package io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.NumberFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.NumberFilterParams;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public class AgNumberColumnFilter<N extends Number> extends SimpleFilter<N, NumberFilterModel, NumberFilterParams> {

    @Override
    @NonNull
    public NumberFilterModel recognizeFilterModel(@NonNull Map<String, Object> model) {
        NumberFilterModel numberFilter = new NumberFilterModel();
        numberFilter.setType(SimpleFilterModelType.valueOf(model.get("type").toString()));
        numberFilter.setFilter(Optional.ofNullable(model.get("filter")).map(Object::toString).map(BigDecimal::new).orElse(null));
        numberFilter.setFilterTo(Optional.ofNullable(model.get("filterTo")).map(Object::toString).map(BigDecimal::new).orElse(null));
        return numberFilter;
    }

    @Override
    @NonNull
    public NumberFilterParams getDefaultFilterParams() {
        return NumberFilterParams.builder().build();
    }


    @Override
    @NonNull
    protected Predicate toPredicate(@NonNull CriteriaBuilder cb, @NonNull Expression<N> numberExpression, @NonNull NumberFilterModel filterModel) {
        Predicate predicate;
        
        switch (filterModel.getType()) {
            case empty: case blank: {
                predicate = cb.isNull(numberExpression);
                break;
            }
            case notBlank: {
                predicate = cb.isNotNull(numberExpression);
                break;
            }
            case equals: {
                predicate = cb.equal(numberExpression, filterModel.getFilter());
                if (filterParams.isIncludeBlanksInEquals()) {
                    predicate = cb.or(predicate, cb.isNull(numberExpression));
                }
                break;
            }
            case notEqual: {
                predicate = cb.notEqual(numberExpression, filterModel.getFilter());
                if (filterParams.isIncludeBlanksInNotEqual()) {
                    predicate = cb.or(predicate, cb.isNull(numberExpression));
                }
                break;
            }
            case lessThan: {
                predicate = cb.lt(numberExpression, filterModel.getFilter());
                if (filterParams.isIncludeBlanksInLessThan()) {
                    predicate = cb.or(predicate, cb.isNull(numberExpression));
                }
                break;
            }
            case lessThanOrEqual: {
                predicate = cb.le(numberExpression, filterModel.getFilter());
                if (filterParams.isIncludeBlanksInLessThan()) {
                    predicate = cb.or(predicate, cb.isNull(numberExpression));
                }
                break;
            }
            case greaterThan: {
                predicate = cb.gt(numberExpression, filterModel.getFilter());
                if (filterParams.isIncludeBlanksInGreaterThan()) {
                    predicate = cb.or(predicate, cb.isNull(numberExpression));
                }
                break;
            }
            case greaterThanOrEqual: {
                predicate = cb.ge(numberExpression, filterModel.getFilter());
                if (filterParams.isIncludeBlanksInGreaterThan()) {
                    predicate = cb.or(predicate, cb.isNull(numberExpression));
                }
                break;
            }
            case inRange: {
                if (filterParams.isInRangeInclusive()) {
                    predicate = cb.and(cb.ge(numberExpression, filterModel.getFilter()), cb.le(numberExpression, filterModel.getFilterTo()));
                } else {
                    predicate = cb.and(cb.gt(numberExpression, filterModel.getFilter()), cb.lt(numberExpression, filterModel.getFilterTo()));
                }
                if (filterParams.isIncludeBlanksInRange()) {
                    predicate = cb.or(predicate, cb.isNull(numberExpression));
                }
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected value: " + filterModel.getType());
            }
        }

        return predicate;
    }
}
