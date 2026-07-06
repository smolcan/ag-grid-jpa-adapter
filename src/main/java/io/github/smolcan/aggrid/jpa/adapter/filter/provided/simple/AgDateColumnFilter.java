package io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.DateFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.DateFilterParams;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.NonNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("java:S119")
public abstract class AgDateColumnFilter<DT extends Temporal & Comparable<? super DT>> extends SimpleFilter<DT, DateFilterModel, DateFilterParams> {

    @NonNull
    public static AgLocalDateColumnFilter forLocalDate() {
        return new AgLocalDateColumnFilter();
    }

    @NonNull
    public static AgLocalDateTimeColumnFilter forLocalDateTime() {
        return new AgLocalDateTimeColumnFilter();
    }

    @NonNull
    public static AgInstantColumnFilter forInstant(@NonNull ZoneId zone) {
        return new AgInstantColumnFilter(zone);
    }

    @Override
    @NonNull
    public DateFilterModel recognizeFilterModel(@NonNull Map<String, Object> filterModel) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        DateFilterModel dateFilterModel = new DateFilterModel();
        dateFilterModel.setType(SimpleFilterModelType.valueOf(filterModel.get("type").toString()));
        dateFilterModel.setDateFrom(Optional.ofNullable(filterModel.get("dateFrom")).map(Object::toString).map(d -> LocalDateTime.parse(d, dateTimeFormatter)).orElse(null));
        dateFilterModel.setDateTo(Optional.ofNullable(filterModel.get("dateTo")).map(Object::toString).map(d -> LocalDateTime.parse(d, dateTimeFormatter)).orElse(null));

        return dateFilterModel;
    }

    @Override
    @NonNull
    public DateFilterParams getDefaultFilterParams() {
        return DateFilterParams.builder().build();
    }

    @NonNull
    protected abstract DT convertFromLocalDateTime(@NonNull LocalDateTime dateTime);

    @Override
    @NonNull
    protected Predicate toPredicate(@NonNull CriteriaBuilder cb, @NonNull Expression<DT> expression, @NonNull DateFilterModel filterModel) {
        this.filterParams.validateDate(filterModel.getDateFrom());
        this.filterParams.validateDate(filterModel.getDateTo());
        Predicate predicate;
        switch (filterModel.getType()) {
            case empty: case blank: {
                predicate = cb.isNull(expression);
                break;
            }
            case notBlank: {
                predicate = cb.isNotNull(expression);
                break;
            }
            case equals: {
                Objects.requireNonNull(filterModel.getDateFrom());
                predicate = cb.equal(expression, this.convertFromLocalDateTime(filterModel.getDateFrom()));
                if (filterParams.isIncludeBlanksInEquals()) {
                    predicate = cb.or(predicate, cb.isNull(expression));
                }
                break;
            }
            case notEqual: {
                Objects.requireNonNull(filterModel.getDateFrom());
                predicate = cb.notEqual(expression, this.convertFromLocalDateTime(filterModel.getDateFrom()));
                if (filterParams.isIncludeBlanksInNotEqual()) {
                    predicate = cb.or(predicate, cb.isNull(expression));
                }
                break;
            }
            case lessThan: {
                Objects.requireNonNull(filterModel.getDateFrom());
                predicate = cb.lessThan(expression, this.convertFromLocalDateTime(filterModel.getDateFrom()));
                if (filterParams.isIncludeBlanksInLessThan()) {
                    predicate = cb.or(predicate, cb.isNull(expression));
                }
                break;
            }
            case lessThanOrEqual: {
                Objects.requireNonNull(filterModel.getDateFrom());
                predicate = cb.lessThanOrEqualTo(expression, this.convertFromLocalDateTime(filterModel.getDateFrom()));
                if (filterParams.isIncludeBlanksInLessThan()) {
                    predicate = cb.or(predicate, cb.isNull(expression));
                }
                break;
            }
            case greaterThan: {
                Objects.requireNonNull(filterModel.getDateFrom());
                predicate = cb.greaterThan(expression, this.convertFromLocalDateTime(filterModel.getDateFrom()));
                if (filterParams.isIncludeBlanksInGreaterThan()) {
                    predicate = cb.or(predicate, cb.isNull(expression));
                }
                break;
            }
            case greaterThanOrEqual: {
                Objects.requireNonNull(filterModel.getDateFrom());
                predicate = cb.greaterThanOrEqualTo(expression, this.convertFromLocalDateTime(filterModel.getDateFrom()));
                if (filterParams.isIncludeBlanksInGreaterThan()) {
                    predicate = cb.or(predicate, cb.isNull(expression));
                }
                break;
            }
            case inRange: {
                Objects.requireNonNull(filterModel.getDateFrom());
                Objects.requireNonNull(filterModel.getDateTo());
                DT dateFrom = this.convertFromLocalDateTime(filterModel.getDateFrom());
                DT dateTo = this.convertFromLocalDateTime(filterModel.getDateTo());
                if (filterParams.isInRangeInclusive()) {
                    predicate = cb.and(cb.greaterThanOrEqualTo(expression, dateFrom), cb.lessThanOrEqualTo(expression, dateTo));
                } else {
                    predicate = cb.and(cb.greaterThan(expression, dateFrom), cb.lessThan(expression, dateTo));
                }
                if (filterParams.isIncludeBlanksInRange()) {
                    predicate = cb.or(predicate, cb.isNull(expression));
                }
                break;
            }

            // relative times
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
                predicate = this.createNamedAndRelativeDateRangePredicate(cb, expression, filterModel.getType());
                break;
            }

            default: {
                throw new IllegalStateException("Unexpected value: " + filterModel.getType());
            }
        }

        return predicate;
    }


    @NonNull
    protected Predicate createNamedAndRelativeDateRangePredicate(@NonNull CriteriaBuilder cb, @NonNull Expression<DT> expression, @NonNull SimpleFilterModelType type) {
        if (!this.filterParams.getFilterOptions().contains(type)) {
            throw new IllegalArgumentException("Tried to use relative date filter " + type + ", but filter is not present in date filter params -> filter options");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.with(LocalTime.MIN);

        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        LocalDateTime startOfCurrentWeek = startOfToday.minusDays(now.get(weekFields.dayOfWeek()) - 1L);

        LocalDateTime startOfCurrentMonth = now.withDayOfMonth(1).with(LocalTime.MIN);

        int currentMonth = now.getMonthValue();
        int currentQuarter = ((currentMonth - 1) / 3) + 1;
        int startMonthOfQuarter = (currentQuarter - 1) * 3 + 1;
        LocalDateTime startOfCurrentQuarter = now.withMonth(startMonthOfQuarter).withDayOfMonth(1).with(LocalTime.MIN);

        LocalDateTime startOfCurrentYear = now.withDayOfYear(1).with(LocalTime.MIN);

        LocalDateTime dateFrom;
        LocalDateTime dateTo;
        switch (type) {
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
                throw new IllegalStateException("Unexpected value: " + type);
        }

        // validate before using
        this.filterParams.validateDate(dateFrom);
        this.filterParams.validateDate(dateTo);

        Predicate predicate = cb.and(
                // 	Time Range Start >=
                cb.greaterThanOrEqualTo(expression, this.convertFromLocalDateTime(dateFrom)),
                // 	Time Range End <
                cb.lessThan(expression, this.convertFromLocalDateTime(dateTo))
        );

        if (this.filterParams.isIncludeBlanksInRange()) {
            predicate = cb.or(predicate, cb.isNull(expression));
        }

        return predicate;
    }


    public static class AgLocalDateColumnFilter extends AgDateColumnFilter<LocalDate> {

        @Override
        @NonNull
        protected LocalDate convertFromLocalDateTime(@NonNull LocalDateTime dateTime) {
            return dateTime.toLocalDate();
        }
    }


    public static class AgLocalDateTimeColumnFilter extends AgDateColumnFilter<LocalDateTime> {

        @Override
        @NonNull
        protected LocalDateTime convertFromLocalDateTime(@NonNull LocalDateTime dateTime) {
            return dateTime;
        }
    }


    public static class AgInstantColumnFilter extends AgDateColumnFilter<Instant> {

        @NonNull
        private final ZoneId zone;

        public AgInstantColumnFilter(@NonNull ZoneId zone) {
            this.zone = zone;
        }

        @Override
        @NonNull
        protected Instant convertFromLocalDateTime(@NonNull LocalDateTime dateTime) {
            return dateTime.atZone(this.zone).toInstant();
        }
    }
}
