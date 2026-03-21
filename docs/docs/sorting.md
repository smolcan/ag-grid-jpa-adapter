---
sidebar_position: 3
---

# Sorting

Sorting can be either **ascending** or **descending**, represented by the enum [SortDirection](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/request/SortDirection.java).

## Disabling Sorting

To prevent sorting on a column, set the `sortable` property in the column definition to `false`:

```java
ColDef priceColumn = ColDef.builder()
    .field("price")
    .sortable(false)
    .build();
```


## Grid using server side sorting

- sorting `Trade ID` works
- sorting `Product` is turned off
- sorting `Portfolio` is turned on in client, but turned off in server: `Server throws exception`
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/sorting-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/SortingService.java)

import ShowSqlMonitor from './show-sql-monitor';
import SortingGrid from './sorting-grid';
import SortingAbsoluteGrid from './sorting-absolute-grid';
import LazyGrid from './lazy-grid';

<ShowSqlMonitor serviceUrls={['/docs/sorting/getRows']}>
    <LazyGrid>
        <SortingGrid></SortingGrid>
    </LazyGrid>
</ShowSqlMonitor>

## Absolute Sorting

Absolute Sorting orders numeric values by their magnitude, ignoring their sign. For example, with absolute sorting ascending, `-100` would sort after `5` because `|-100| = 100 > 5`.

This is useful for financial data where you want to rank by the size of a value regardless of whether it is positive or negative (e.g., P&L, trade deltas).

Absolute sorting is enabled on the frontend by setting the column sort type to `'absolute'` in AG Grid. The adapter automatically applies `ORDER BY ABS(column)` when it receives a sort request with type `absolute`.

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/sorting-absolute-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/SortingService.java)

<ShowSqlMonitor serviceUrls={['/docs/sorting/absolute/getRows']}>
<LazyGrid>
<SortingAbsoluteGrid></SortingAbsoluteGrid>
</LazyGrid>
</ShowSqlMonitor>

