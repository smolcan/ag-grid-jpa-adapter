---
sidebar_position: 7
---

# Pivoting
Pivoting breaks down data in an additional dimension — it transforms distinct values from one column into separate result columns, each showing an aggregate for that value.

## Enabling Pivoting
To make a column available for pivoting, set the `enablePivot` parameter to `true` on `ColDef`,
otherwise a pivoting attempt on this column will result in a runtime exception.

```java
var priceColumn = ColDef.builder(Entity_.price)
    .enablePivot(true)
    .build();
```

import PivotingGrid from './pivoting-grid';
import PivotingFilteringGrid from './pivoting-filtering-grid';
import PivotingLimitColGenGrid from './pivoting-limit-col-gen-grid';
import ShowSqlMonitor from './show-sql-monitor';
import LazyGrid from './lazy-grid';

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/pivoting-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/PivotingService.java)

<ShowSqlMonitor serviceUrls={['/docs/pivoting/getRows']}>
<LazyGrid>
    <PivotingGrid></PivotingGrid>
</LazyGrid>
</ShowSqlMonitor>

## Filtering
Filters work the same way as without pivoting, they restrict the rows the values are aggregated from.
In pivot mode they also decide which columns are generated, while expanding a group does not.
Filters on aggregated values behave as described in
[Filtering for Aggregated Values](./aggregation.md#filtering-for-aggregated-values).

`pivotMaxGeneratedColumns` is checked against the columns the filtered rows generate, so a filter can bring
a request back under the limit.

### Example - Filtering

In pivot mode the grid displays the generated columns, so the columns of your grid are reached through the
**Filters** tool panel on the right.

- `Book` is the pivot column - filtering it changes which columns are generated
- `Product` and `Portfolio` are the row group columns - filtering them changes which rows are returned
- `Current Value` and `Previous Value` are aggregated - filtering them drops rows before they are aggregated
- a generated column (`Book 1_currentValue`) is filtered from its header menu, by its aggregated value
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/pivoting-filtering-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/PivotingService.java)

<ShowSqlMonitor serviceUrls={['/docs/pivoting/filtering/getRows']}>
<LazyGrid>
    <PivotingFilteringGrid></PivotingFilteringGrid>
</LazyGrid>
</ShowSqlMonitor>

## Best Practices - Limiting Column Generation
When pivoting, changes in data, aggregation or pivot columns can cause the number of generated columns to scale exponentially.
To prevent this from happening, you can set the `pivotMaxGeneratedColumns` option on `QueryBuilder`.
Server will count number of columns to be generated in advance.

When the grid generates a number of pivot columns exceeding this value, it halts column generation and throws 
the [`OnPivotMaxColumnsExceededException`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/exceptions/OnPivotMaxColumnsExceededException.java).

```java
this.queryBuilder = QueryBuilder.builder(Entity.class, Entity_.id, entityManager)
                .colDefs(
                        // colDefs
                )
                .pivotMaxGeneratedColumns(1000)
                .build();
```

### Example - Limiting Column Generation

- Column generation is checked on server (limit 10 columns)
- Adding `Bid Type` pivoting will result in error on server
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/pivoting-limit-col-gen-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/PivotingService.java)

<ShowSqlMonitor serviceUrls={['/docs/pivoting/limit-col-gen/getRows']}>
<LazyGrid>
    <PivotingLimitColGenGrid></PivotingLimitColGenGrid>
</LazyGrid>
</ShowSqlMonitor>
