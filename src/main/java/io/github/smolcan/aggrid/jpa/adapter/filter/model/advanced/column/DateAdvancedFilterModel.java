package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.column;

import io.github.smolcan.aggrid.jpa.adapter.column.ColumnSource;
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
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Objects;

@Getter
@Setter
public class DateAdvancedFilterModel<E, T> extends ColumnAdvancedFilterModel<E, T> {

    @Setter(onMethod_ = {@NonNull})
    private ScalarAdvancedFilterModelType type;
    private LocalDate filter;
    private LocalDate filterTo;
    @NonNull
    private DateFilterParams filterParams = DateFilterParams.builder().build();
    
    public DateAdvancedFilterModel(@NonNull ColumnSource<? super E, T> columnField) {
        super("date", columnField);
    }
    
    @Override
    @NonNull
    public Predicate toPredicate(@NonNull CriteriaBuilder cb, @NonNull Root<? extends E> root) {
        this.filterParams.validateDate(this.filter);
        this.filterParams.validateDate(this.filterTo);
        Predicate predicate;
        
        Expression<LocalDate> path = this.getColumnField().getExpression(cb, root).as(LocalDate.class);
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
            case inRange: {
                Objects.requireNonNull(filter);
                Objects.requireNonNull(filterTo);
                if (filterParams.isInRangeInclusive()) {
                    predicate = cb.and(cb.greaterThanOrEqualTo(path, filter), cb.lessThanOrEqualTo(path, filterTo));
                } else {
                    predicate = cb.and(cb.greaterThan(path, filter), cb.lessThan(path, filterTo));
                }
                if (filterParams.isIncludeBlanksInRange()) {
                    predicate = cb.or(predicate, cb.isNull(path));
                }
                break;
            }

            case today:
            case yesterday:
            case tomorrow:
            case thisWeek:
            case lastWeek:
            case nextWeek:
            case thisMonth:
            case lastMonth:
            case nextMonth:
            case thisQuarter:
            case lastQuarter:
            case nextQuarter:
            case thisYear:
            case lastYear:
            case nextYear:
            case yearToDate:
            case last7Days:
            case last30Days:
            case last90Days:
            case last6Months:
            case last12Months:
            case last24Months: {
                predicate = this.createNamedAndRelativeDateRangePredicate(cb, path);
                break;
            }
            
            default: {
                throw new IllegalStateException("Unexpected value: " + this.type);
            }
        }
        
        return predicate;
    }

    @NonNull
    protected Predicate createNamedAndRelativeDateRangePredicate(@NonNull CriteriaBuilder cb, @NonNull Expression<LocalDate> expression) {
        LocalDate startOfToday = LocalDate.now(this.filterParams.getZoneId());

        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        LocalDate startOfCurrentWeek = startOfToday.minusDays(startOfToday.get(weekFields.dayOfWeek()) - 1L);

        LocalDate startOfCurrentMonth = startOfToday.withDayOfMonth(1);

        int currentMonth = startOfToday.getMonthValue();
        int currentQuarter = ((currentMonth - 1) / 3) + 1;
        int startMonthOfQuarter = (currentQuarter - 1) * 3 + 1;
        LocalDate startOfCurrentQuarter = startOfCurrentMonth.withMonth(startMonthOfQuarter);

        LocalDate startOfCurrentYear = startOfToday.withDayOfYear(1);

        LocalDate dateFrom;
        LocalDate dateTo;
        switch (this.type) {
            case today:
                dateFrom = startOfToday;
                dateTo = startOfToday.plusDays(1);
                break;
            case yesterday:
                dateFrom = startOfToday.minusDays(1);
                dateTo = startOfToday;
                break;
            case tomorrow:
                dateFrom = startOfToday.plusDays(1);
                dateTo = startOfToday.plusDays(2);
                break;
            case thisWeek:
                dateFrom = startOfCurrentWeek;
                dateTo = startOfCurrentWeek.plusDays(7);
                break;
            case lastWeek:
                dateFrom = startOfCurrentWeek.minusDays(7);
                dateTo = startOfCurrentWeek;
                break;
            case nextWeek:
                dateFrom = startOfCurrentWeek.plusDays(7);
                dateTo = startOfCurrentWeek.plusDays(14);
                break;
            case thisMonth:
                dateFrom = startOfCurrentMonth;
                dateTo = startOfCurrentMonth.plusMonths(1);
                break;
            case lastMonth:
                dateFrom = startOfCurrentMonth.minusMonths(1);
                dateTo = startOfCurrentMonth;
                break;
            case nextMonth:
                dateFrom = startOfCurrentMonth.plusMonths(1);
                dateTo = startOfCurrentMonth.plusMonths(2);
                break;
            case thisQuarter:
                dateFrom = startOfCurrentQuarter;
                dateTo = startOfCurrentQuarter.plusMonths(3);
                break;
            case lastQuarter:
                dateFrom = startOfCurrentQuarter.minusMonths(3);
                dateTo = startOfCurrentQuarter;
                break;
            case nextQuarter:
                dateFrom = startOfCurrentQuarter.plusMonths(3);
                dateTo = startOfCurrentQuarter.plusMonths(6);
                break;
            case thisYear:
                dateFrom = startOfCurrentYear;
                dateTo = startOfCurrentYear.plusYears(1);
                break;
            case lastYear:
                dateFrom = startOfCurrentYear.minusYears(1);
                dateTo = startOfCurrentYear;
                break;
            case nextYear:
                dateFrom = startOfCurrentYear.plusYears(1);
                dateTo = startOfCurrentYear.plusYears(2);
                break;
            case yearToDate:
                dateFrom = startOfCurrentYear;
                dateTo = startOfToday.plusDays(1);
                break;
            case last7Days:
                dateFrom = startOfToday.minusDays(7);
                dateTo = startOfToday.plusDays(1);
                break;
            case last30Days:
                dateFrom = startOfToday.minusDays(30);
                dateTo = startOfToday.plusDays(1);
                break;
            case last90Days:
                dateFrom = startOfToday.minusDays(90);
                dateTo = startOfToday.plusDays(1);
                break;
            case last6Months:
                dateFrom = startOfToday.minusMonths(6);
                dateTo = startOfToday.plusDays(1);
                break;
            case last12Months:
                dateFrom = startOfToday.minusMonths(12);
                dateTo = startOfToday.plusDays(1);
                break;
            case last24Months:
                dateFrom = startOfToday.minusMonths(24);
                dateTo = startOfToday.plusDays(1);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + this.type);
        }

        // validate before using
        this.filterParams.validateDate(dateFrom);
        this.filterParams.validateDate(dateTo);

        Predicate predicate = cb.and(
                // 	Time Range Start >=
                cb.greaterThanOrEqualTo(expression, dateFrom),
                // 	Time Range End <
                cb.lessThan(expression, dateTo)
        );

        if (this.filterParams.isIncludeBlanksInRange()) {
            predicate = cb.or(predicate, cb.isNull(expression));
        }

        return predicate;
    }
}
