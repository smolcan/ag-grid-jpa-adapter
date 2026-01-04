---
sidebar_position: 3
---

# Sorting

Sorting can be either **ascending** or **descending**, represented by the enum [SortType](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/request/SortType.java).

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

## Absolute sorting

Absolute Sorting enables sorting numeric values based on their magnitude, ignoring their sign.

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/sorting-absolute-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/SortingService.java)

<ShowSqlMonitor serviceUrls={['/docs/sorting/absolute/getRows']}>
<LazyGrid>
<SortingAbsoluteGrid></SortingAbsoluteGrid>
</LazyGrid>
</ShowSqlMonitor>

