---
sidebar_position: 6
---

# Aggregation
Apply provided functions to values to calculate group values in the grid.

Backend source code of example grids available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/AggregationService.java)


## Enabling Aggregation
To make column available for aggregation, set the `enableValue` parameter to `true` on `ColDef`,
otherwise aggregation attempt on this column will result to runtime exception.

To limit which aggregation functions ([AggregationFunction](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/request/AggregationFunction.java)) 
can be used on the column, provide array of allowed functions (otherwise all the aggregation functions are allowed.)

```java
ColDef priceColumn = ColDef.builder()
    .field("price")
    .enableValue(true)
    .allowedAggFuncs(AggregationFunction.avg, AggregationFunction.count)
    // or just use string names
    // .allowedAggFuncs("avg", "count")
    .build();
```

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/aggregation-grid.tsx)

import GridLoadingMessage from './grid-loading-message';
import AggregationGrid from './aggregation-grid';
import AggregationCustomFunctionGrid from './aggregation-custom-function-grid';
import AggregationGridGroupFiltering from './aggregation-grid-group-filtering';
import AggregationGridSuppressFilteredOnly from './aggregation-grid-suppress-filtered-only';

<GridLoadingMessage serviceUrls={['/docs/aggregation/getRows']}>
    <AggregationGrid></AggregationGrid>
</GridLoadingMessage>


**⚠️ Disclaimer**
Currently aggregation functions `first` and `last` are not supported in JPA. Using these functions will result in runtime exception.
Supported functions are: `avg`, `sum`, `min`, `max`, `count`.

## Custom Aggregation Functions

In addition to the built-in functions, the adapter allows you to register custom aggregation functions or overwrite existing ones. 
This is particularly useful for leveraging database-specific functions (e.g., statistical functions) that are not part of the standard JPA set.

Custom functions are registered within the `QueryBuilder` using the `.registerCustomAggFunction()` method.
The following example demonstrates registering functions specifically for the H2 database.

```java
QueryBuilder<Entity> queryBuilder = QueryBuilder.builder(Entity.class, entityManager)
    .colDefs(colDefs)
    // Registering custom aggregation functions
    .registerCustomAggFunction("bool_and", (cb, expr) -> cb.function("BOOL_AND", Boolean.class, expr))
    .registerCustomAggFunction("stddev_pop", (cb, expr) -> cb.function("STDDEV_POP", BigDecimal.class, expr))
    .registerCustomAggFunction("stddev_samp", (cb, expr) -> cb.function("STDDEV_SAMP", BigDecimal.class, expr))
    .build();
```

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/aggregation-custom-function-grid.tsx)

<GridLoadingMessage serviceUrls={['/docs/aggregation/custom-agg-func/getRows']}>
    <AggregationCustomFunctionGrid></AggregationCustomFunctionGrid>
</GridLoadingMessage>

## Aggregation - Filtering
Filtering can be configured to impact aggregate values in the grid.

### Ignore Filters when Aggregating
When using Filters and Aggregations together, the aggregated values reflect only the rows which have passed the filter. 
This can be changed to instead ignore applied filters by using the `suppressAggFilteredOnly` grid option.

```java
QueryBuilder<Entity> queryBuilder = QueryBuilder.builder(Entity.class, entityManager)
    .colDefs(
        // ...col defs
    )
    .suppressAggFilteredOnly(true)
    .build();
```

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/aggregation-grid-suppress-filtered-only.tsx)

<GridLoadingMessage serviceUrls={['/docs/aggregation/suppress-agg-filtered-only/getRows']}>
    <AggregationGridSuppressFilteredOnly></AggregationGridSuppressFilteredOnly>
</GridLoadingMessage>

### Filtering for Aggregated Values
The grid only applies filters to leaf level rows, this can be toggled to instead also apply filtering to group rows by enabling the `groupAggFiltering` grid option, 
allowing filters to also apply against the aggregated values.

```java
QueryBuilder<Entity> queryBuilder = QueryBuilder.builder(Entity.class, entityManager)
    .colDefs(
        // ...col defs
    )
    .groupAggFiltering(true)
    .build();
```

- When a group row passes a filter, it also includes all of its descendent rows in the filtered results.
- The `suppressAggFilteredOnly` grid option will be implicitly enabled.
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/aggregation-grid-group-filtering.tsx)

<GridLoadingMessage serviceUrls={['/docs/aggregation/group-agg-filtering/getRows']}>
    <AggregationGridGroupFiltering></AggregationGridGroupFiltering>
</GridLoadingMessage>