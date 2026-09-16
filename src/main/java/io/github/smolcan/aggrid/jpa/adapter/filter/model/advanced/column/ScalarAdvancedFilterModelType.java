package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.column;

@SuppressWarnings("java:S115")
public enum ScalarAdvancedFilterModelType {
    equals,
    notEqual,
    greaterThan,
    greaterThanOrEqual,
    lessThan,
    lessThanOrEqual,
    inRange,
    blank,
    notBlank,

    // built-in named and relative date ranges
    today,
    yesterday,
    tomorrow,
    thisWeek,
    lastWeek,
    nextWeek,
    thisMonth,
    lastMonth,
    nextMonth,
    thisQuarter,
    lastQuarter,
    nextQuarter,
    thisYear,
    lastYear,
    nextYear,
    yearToDate,
    last7Days,
    last30Days,
    last90Days,
    last6Months,
    last12Months,
    last24Months,
    ;
}
