---
sidebar_position: 4
---

# Set Filter
The Set Filter takes inspiration from Excel's AutoFilter and allows filtering on sets of data.

## Using Set Filter
Set filter is represented by class [AgSetColumnFilter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/AgSetColumnFilter.java).

```java
ColDef colDef = ColDef.builder()
    .field("product")
    .filter(new AgSetColumnFilter())
    .build()
```

## Set Filter Parameters
Set Filters are configured though the filter params ([SetFilterParams](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/SetFilterParams.java) class)

| Property                      | Type                                                                  | Default    | Description                                                                                                                                                                                                      |
|-------------------------------|-----------------------------------------------------------------------|------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`caseSensitive`**                | `boolean`                                                             | `false`    | By default, set filtering is case-insensitive. Set this to true to make text filtering case-sensitive.                                                                                                           |
| **`textFormatter`**                  | `BiFunction<CriteriaBuilder, Expression<String>, Expression<String>>` | —           | Formats the text before applying the filter compare logic. Useful if you want to substitute accented characters, for example. Works only if the column is string type. Same as in [text filter](text-filter.md#text-formatter). |


Example of using filter parameters.
```java
ColDef colDef = ColDef.builder()
    .field("product")
    .filter(new AgSetColumnFilter()
        .filterParams(
            SetFilterParams.builder()
                .caseSensitive(false)
                .textFormatter((cb, expr) => {
                    Expression<String> newExpression = expr;
                    // lower input
                    newExpression = cb.lower(newExpression);
                    // Remove accents
                    newExpression = cb.function("TRANSLATE", String.class, newExpression,
                        cb.literal("áéíóúÁÉÍÓÚüÜñÑ"),
                        cb.literal("aeiouAEIOUuUnN"));
                    
                    return newExpression;
                })
                .build()
        )
    )
    .build()
```


## Set Filter Model
Set filter model is represented by [SetFilterModel](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/SetFilterModel.java) class.


## Grid using Server Side Set Filter
- `Product` uses default set filter
- `Portfolio` is case-sensitive
- `Book` uses set filter with custom `textFormatter` - accent removal
- `Submitter Id` uses set filter with numbers
- `Birth Date` uses set filter with dates
- `Is Sold` uses set filter with boolean values + Blank value can be selected
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/filtering/column-filter/set-filter-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/SetFilterService.java)

import GridLoadingMessage from './../../grid-loading-message';
import SetFilterGrid from './set-filter-grid';

<GridLoadingMessage>
<SetFilterGrid></SetFilterGrid>
</GridLoadingMessage>