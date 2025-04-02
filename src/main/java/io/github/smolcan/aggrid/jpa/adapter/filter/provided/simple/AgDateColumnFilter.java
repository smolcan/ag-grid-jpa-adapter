package io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.DateFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.DateFilterParams;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
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
    public DateFilterParams getDefaultFilterParams() {
        return DateFilterParams.builder().build();
    }

    @Override
    protected Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression, DateFilterModel filterModel) {
        this.validateDate(filterModel.getDateFrom());
        this.validateDate(filterModel.getDateTo());
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
                Objects.requireNonNull(filterModel.getDateFrom());
                predicate = cb.equal(dateExpression, filterModel.getDateFrom());
                if (filterParams.isIncludeBlanksInEquals()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            case notEqual: {
                Objects.requireNonNull(filterModel.getDateFrom());
                predicate = cb.notEqual(dateExpression, filterModel.getDateFrom());
                if (filterParams.isIncludeBlanksInNotEqual()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            case lessThan: {
                Objects.requireNonNull(filterModel.getDateFrom());
                predicate = cb.lessThan(dateExpression, filterModel.getDateFrom());
                if (filterParams.isIncludeBlanksInLessThan()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            case lessThanOrEqual: {
                Objects.requireNonNull(filterModel.getDateFrom());
                predicate = cb.lessThanOrEqualTo(dateExpression, filterModel.getDateFrom());
                if (filterParams.isIncludeBlanksInLessThan()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            case greaterThan: {
                Objects.requireNonNull(filterModel.getDateFrom());
                predicate = cb.greaterThan(dateExpression, filterModel.getDateFrom());
                if (filterParams.isIncludeBlanksInGreaterThan()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            case greaterThanOrEqual: {
                Objects.requireNonNull(filterModel.getDateFrom());
                predicate = cb.greaterThanOrEqualTo(dateExpression, filterModel.getDateFrom());
                if (filterParams.isIncludeBlanksInGreaterThan()) {
                    predicate = cb.or(predicate, cb.isNull(dateExpression));
                }
                break;
            }
            case inRange: {
                Objects.requireNonNull(filterModel.getDateFrom());
                Objects.requireNonNull(filterModel.getDateTo());
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
    
    
    private void validateDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return;
        }
        
        int year = dateTime.getYear();
        if (this.filterParams.getMaxValidYear() != null) {
            if (year > this.filterParams.getMaxValidYear()) {
                throw new IllegalArgumentException("Max valid year exceeded!");
            }
        }
        if (this.filterParams.getMinValidYear() != null) {
            if (year < this.filterParams.getMinValidYear()) {
                throw new IllegalArgumentException("Min valid year exceeded!");
            }
        }
        
        LocalDate date = dateTime.toLocalDate();
        if (this.filterParams.getMaxValidDate() != null) {
            if (date.isAfter(this.filterParams.getMaxValidDate())) {
                throw new IllegalArgumentException("Max valid date exceeded");
            }
        }
        if (this.filterParams.getMinValidDate() != null) {
            if (date.isBefore(this.filterParams.getMinValidDate())) {
                throw new IllegalArgumentException("Min valid date exceeded");
            }
        }
    }
}
