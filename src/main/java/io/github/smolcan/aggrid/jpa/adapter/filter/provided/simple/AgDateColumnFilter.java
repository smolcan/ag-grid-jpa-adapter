package io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.DateFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.DateFilterParams;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

public class AgDateColumnFilter extends SimpleFilter<DateFilterModel, DateFilterParams> {
    
    @Override
    public DateFilterModel recognizeFilterModel(Map<String, Object> filterModel) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        DateFilterModel dateFilterModel = new DateFilterModel();
        dateFilterModel.setType(SimpleFilterModelType.valueOf(filterModel.get("type").toString()));
        dateFilterModel.setDateFrom(Optional.ofNullable(filterModel.get("dateFrom")).map(Object::toString).map(d -> LocalDateTime.parse(d, dateTimeFormatter)).orElse(null));
        dateFilterModel.setDateTo(Optional.ofNullable(filterModel.get("dateTo")).map(Object::toString).map(d -> LocalDateTime.parse(d, dateTimeFormatter)).orElse(null));

        return dateFilterModel;
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression, DateFilterModel filterModel, DateFilterParams filterParams) {
        Predicate predicate;
        Expression<LocalDateTime> dateExpression = expression.as(LocalDateTime.class);
        switch (filterModel.getType()) {
            case empty: case blank: {
                predicate = cb.isNull(dateExpression);
                break;
            }
            case notBlank: {
                predicate = cb.isNotNull(dateExpression);
                break;
            }
            case equals: {
                predicate = cb.equal(dateExpression, filterModel.getDateFrom());
                if (filterParams.isIncludeBlanksInEquals()) {
                    predicate = cb.or(predicate, cb.isNull(predicate));
                }
                break;
            }
            case notEqual: {
                predicate = cb.notEqual(dateExpression, filterModel.getDateFrom());
                if (filterParams.isIncludeBlanksInNotEqual()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            case lessThan: {
                predicate = cb.lessThan(dateExpression, filterModel.getDateFrom());
                if (filterParams.isIncludeBlanksInLessThan()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            case lessThanOrEqual: {
                predicate = cb.lessThanOrEqualTo(dateExpression, filterModel.getDateFrom());
                if (filterParams.isIncludeBlanksInLessThan()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            case greaterThan: {
                predicate = cb.greaterThan(dateExpression, filterModel.getDateFrom());
                if (filterParams.isIncludeBlanksInGreaterThan()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            case greaterThanOrEqual: {
                predicate = cb.greaterThanOrEqualTo(dateExpression, filterModel.getDateFrom());
                if (filterParams.isIncludeBlanksInGreaterThan()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            case inRange: {
                if (filterParams.isInRangeInclusive()) {
                    predicate = cb.and(cb.greaterThanOrEqualTo(dateExpression, filterModel.getDateFrom()), cb.lessThanOrEqualTo(dateExpression, filterModel.getDateTo()));
                } else {
                    predicate = cb.and(cb.greaterThan(dateExpression, filterModel.getDateFrom()), cb.lessThan(dateExpression, filterModel.getDateTo()));
                }
                if (filterParams.isIncludeBlanksInRange()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
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
