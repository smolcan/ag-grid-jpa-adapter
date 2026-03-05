---
sidebar_position: 3
---

# Date Filter
Date Filters allow you to filter date data.

## Using Date Filter
Date filter is represented by class [AgDateColumnFilter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/simple/AgDateColumnFilter.java).

```java
ColDef colDef = ColDef.builder()
    .field("birthDate")
    .filter(new AgDateColumnFilter())
    .build()
```


## Date Filter Parameters
Date Filters are configured though the filter params ([DateFilterParams](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/DateFilterParams.java) class)

| Property                      | Type        | Default | Description                                                                                                                           |
|-------------------------------|-------------|---------|---------------------------------------------------------------------------------------------------------------------------------------|
| **`inRangeInclusive`** | `boolean`   | `false` | If `true`, the `'inRange'` filter option will include values equal to the start and end of the range.                                 |
| **`includeBlanksInEquals`**                | `boolean`   | `false` | If `true`, blank (`null`) values will pass the `'equals'` filter option.                                                              |
| **`includeBlanksInNotEqual`**                  | `boolean`   | `false` | If `true`, blank (`null`) values will pass the `'notEqual'` filter option.                                                            |
| **`includeBlanksInLessThan`**         | `boolean`   | `false` | If `true`, blank (`null`) values will pass the `'lessThan'` and `'lessThanOrEqual'` filter options.                                   |
| **`includeBlanksInGreaterThan`**         | `boolean`   | `false` | If `true`, blank (`null`) values will pass the `'greaterThan'` and `'greaterThanOrEqual'` filter options.                             |
| **`includeBlanksInRange`**         | `boolean`   | `false` | If `true`, blank (`null`) values will pass the `'inRange'` filter option.                                                             |
| **`maxValidDate`**                  | `LocalDate` | -       | The maximum valid date that can be entered in the filter. If set, this will override `maxValidYear` - the maximum valid year setting. |
| **`maxValidYear`**         | `Integer`   | -       | This is the maximum year that may be entered in a date field for the value to be considered valid.                                    |
| **`minValidDate`**         | `LocalDate` | -       | The minimum valid date that can be entered in the filter. If set, this will override `minValidYear` - the minimum valid year setting.                                                                            |
| **`minValidYear`**         | `Integer`   | `1000`    | This is the minimum year that may be entered in a date field for the value to be considered valid.                                                             |

Example of using filter parameters.
```java
ColDef colDef = ColDef.builder()
    .field("birthDate")
    .filter(new AgDateColumnFilter()
        .filterParams(
            DateFilterParams.builder()
                .inRangeInclusive(true)
                .includeBlanksInEquals(true)
                .includeBlanksInNotEqual(true)
                .includeBlanksInLessThan(true)
                .includeBlanksInGreaterThan(true)
                .includeBlanksInRange(true)
                .maxValidDate(LocalDate.of(2030, Month.DECEMBER, 31))
                .minValidYear(1970)
                .build()
        )
    )
    .build()
```


## Date Filter Model
Date filter model is represented by [DateFilterModel](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/DateFilterModel.java) class.

If more than one Filter Condition is set, then multiple instances of the model are created and wrapped inside a Combined Model ([`CombinedSimpleModel<DateFilterModel>`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/CombinedSimpleModel.java)).


## Grid using Server Side Date Filter

- On column `Birth Date`, `inRangeInclusive`, `includeBlanksInEquals`, `includeBlanksInNotEqual`, `includeBlanksInLessThan`, `includeBlanksInGreaterThan`, `includeBlanksInRange` are all set to `true`
- `minValidYear` is `current - 1` and `maxValidYear` is `current + 1`
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/filtering/column-filter/date-filter-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/DateFilterService.java)

import ShowSqlMonitor from './../../show-sql-monitor';
import DateFilterGrid from './date-filter-grid';
import DateFilterGridRelative from './date-filter-grid-relative';
import LazyGrid from '../../lazy-grid';

<ShowSqlMonitor serviceUrls={['/docs/filtering/column-filter/date-filter/getRows']}>
<LazyGrid>
<DateFilterGrid></DateFilterGrid>
</LazyGrid>
</ShowSqlMonitor>

## Built-in Named & Relative Date Ranges

Date Filter supports a set of predefined named and relative date ranges to make common filtering tasks easier.
These are ideal for users who want to filter by familiar time periods like "Last Week", "Year to Date", or "Next 30 Days"...

### Enabling Built-in Date Ranges

:::info
Relative filters will respect these date filter parameters: `includeBlanksInRange`, `maxValidDate`, `maxValidYear`, `minValidDate` and `minValidYear`.
:::

To enable the built-in date ranges, list them in the `filterOptions` in `filterParams`, 
otherwise relative filtering attempt will result to error.

```java
import static io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType.*;

ColDef colDef = ColDef.builder()
    .field("birthDate")
    .filter(new AgDateColumnFilter()
        .filterParams(
            DateFilterParams.builder()
                .filterOptions(
                    today,
                    yesterday,
                    tomorrow,
                    thisWeek,
                    lastWeek,
                    nextWeek,
                    thisMonth,
                    lastMonth,
                    nextMonth
                    // ... and others
                )
                .build()
        )
    )
    .build()
```

### Available Built-in Named & Relative Date Ranges Options

You can see available options [in official docs](https://www.ag-grid.com/angular-data-grid/filter-date/#available-built-in-date-filter-options).

| Option Name | Option Key | Time Range Start &gt;= | Time Range End &lt; |
| :--- | :--- | :--- | :--- |
| **Today** | `today` | `Start Of Today` | `Start Of Tomorrow` |
| **Yesterday** | `yesterday` | `Start Of Yesterday` | `Start Of Today` |
| **Tomorrow** | `tomorrow` | `Start Of Tomorrow` | `Start Of Day After Tomorrow` |
| **This Week** | `thisWeek` | `Start Of Current Week` | `Start Of Next Week` |
| **Last Week** | `lastWeek` | `Start Of Previous Week` | `Start Of Current Week` |
| **Next Week** | `nextWeek` | `Start Of Next Week` | `Start Of Week After Next` |
| **This Month** | `thisMonth` | `Start Of Current Month` | `Start Of Next Month` |
| **Last Month** | `lastMonth` | `Start Of Previous Month` | `Start Of Current Month` |
| **Next Month** | `nextMonth` | `Start Of Next Month` | `Start Of Month After Next` |
| **This Quarter** | `thisQuarter` | `Start Of Current Quarter` | `Start Of Next Quarter` |
| **Last Quarter** | `lastQuarter` | `Start Of Previous Quarter` | `Start Of Current Quarter` |
| **Next Quarter** | `nextQuarter` | `Start Of Next Quarter` | `Start Of Quarter After Next` |
| **This Year** | `thisYear` | `Start Of Current Year` | `Start Of Next Year` |
| **Last Year** | `lastYear` | `Start Of Previous Year` | `Start Of Current Year` |
| **Next Year** | `nextYear` | `Start Of Next Year` | `Start Of Year After Next` |
| **Year to Date (YTD)** | `yearToDate` | `Start Of Current Year` | `Start Of Tomorrow` |
| **Last 7 Days** | `last7Days` | `Start of the day 7 days before today` | `Start Of Tomorrow` |
| **Last 30 Days** | `last30Days` | `Start Of Today minus 30 days` | `Start Of Tomorrow` |
| **Last 90 Days** | `last90Days` | `Start Of Today minus 90 days` | `Start Of Tomorrow` |
| **Last 6 Months** | `last6Months` | `Start Of Today minus 6 months` | `Start Of Tomorrow` |
| **Last 12 Months** | `last12Months` | `Start Of Today minus 12 months` | `Start Of Tomorrow` |
| **Last 24 Months** | `last24Months` | `Start Of Today minus 24 months` | `Start Of Tomorrow` |

### Example using Built-in Named & Relative Date Ranges

- Column `Birth Date` has relative filters enabled
- Option `nextWeek` is not listed in `filterOptions` on backend, using it will result to server error
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/filtering/column-filter/date-filter-grid-relative.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/DateFilterService.java)

<ShowSqlMonitor serviceUrls={['/docs/filtering/column-filter/date-filter/relative/getRows']}>
<LazyGrid>
<DateFilterGridRelative></DateFilterGridRelative>
</LazyGrid>
</ShowSqlMonitor>
