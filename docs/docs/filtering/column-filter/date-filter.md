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
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/DateFilterService.java)


import DateFilterGrid from './date-filter-grid';

<DateFilterGrid></DateFilterGrid>
