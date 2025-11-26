---
sidebar_position: 2
---

# Number Filter
Number Filters allow you to filter number data.

## Using Number Filter
Text filter is represented by class [AgNumberColumnFilter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/simple/AgNumberColumnFilter.java).

```java
ColDef colDef = ColDef.builder()
    .field("currentValue")
    .filter(new AgNumberColumnFilter())
    .build()
```

## Number Filter Parameters
Number Filters are configured though the filter params ([NumberFilterParams](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/NumberFilterParams.java) class)

| Property                      | Type                                                                  | Default    | Description                                                                                                                                                                                   |
|-------------------------------|-----------------------------------------------------------------------|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`inRangeInclusive`** | `boolean`           | `false`          | If `true`, the `'inRange'` filter option will include values equal to the start and end of the range.                                                                            |
| **`includeBlanksInEquals`**                | `boolean`                                                             | `false`    | If `true`, blank (`null`) values will pass the `'equals'` filter option.                                                                                       |
| **`includeBlanksInNotEqual`**                  | `boolean` | `false`           | If `true`, blank (`null`) values will pass the `'notEqual'` filter option.                                                                 |
| **`includeBlanksInLessThan`**         | `boolean`                                                             | `false`    | If `true`, blank (`null`) values will pass the `'lessThan'` and `'lessThanOrEqual'` filter options. |
| **`includeBlanksInGreaterThan`**         | `boolean`                                            | `false` |  If `true`, blank (`null`) values will pass the `'greaterThan'` and `'greaterThanOrEqual'` filter options.                                                                                                                                                       |
| **`includeBlanksInRange`**         | `boolean`                                            | `false` | If `true`, blank (`null`) values will pass the `'inRange'` filter option.                                                                                                                                                       |

Example of using filter parameters.
```java
ColDef colDef = ColDef.builder()
    .field("currentValue")
    .filter(new AgNumberColumnFilter()
        .filterParams(
            NumberFilterParams.builder()
                .inRangeInclusive(true)
                .includeBlanksInEquals(true)
                .includeBlanksInNotEqual(true)
                .includeBlanksInLessThan(true)
                .includeBlanksInGreaterThan(true)
                .includeBlanksInRange(true)
                .build()
        )
    )
    .build()
```

## Number Filter Model
Text filter model is represented by [NumberFilterModel](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/NumberFilterModel.java) class.

If more than one Filter Condition is set, then multiple instances of the model are created and wrapped inside a Combined Model ([`CombinedSimpleModel<NumberFilterModel>`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/CombinedSimpleModel.java)).


## Grid using Server Side Number Filter

- `Trade Id` uses default filter config
- `Submitter Id` has `inRangeInclusive` set to `true`
- `Submitter Deal ID` has both `includeBlanksInEquals` and `includeBlanksInNotEqual` set to `true`
- `Current Value` has both `includeBlanksInLessThan` and `includeBlanksInGreaterThan` set to `true`
- `Previous Value` has `includeBlanksInRange` set to `true`
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/filtering/column-filter/number-filter-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/NumberFilterService.java)

import GridLoadingMessage from './../../grid-loading-message';
import NumberFilterGrid from './number-filter-grid';

<GridLoadingMessage>
<NumberFilterGrid></NumberFilterGrid>
</GridLoadingMessage>

