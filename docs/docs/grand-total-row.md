---
sidebar_position: 11
---

# Grand Total Row
A grand total row aggregates values across all rows in the grid and is displayed as an extra row at the top or bottom.

To enable it, set `grandTotalRow(true)` on the `QueryBuilder`. Without this flag the adapter will not compute the grand total even if the frontend requests it.

```java
QueryBuilder<Entity, Long, Void> queryBuilder = QueryBuilder.builder(Entity.class, Entity_.id, entityManager)
    .colDefs(...)
    .grandTotalRow(true)
    .build();
```

## Providing Grand Total Data
The grand total row data is returned on `loadSuccessParams.grandTotalData` as part of the standard `getRows` response. The adapter only computes it when the request carries `needsGrandTotal=true`, so make sure to include it in the request body:

```js
body: JSON.stringify({ ...params.request, needsGrandTotal: params.needsGrandTotal })
```

:::info
When `needsGrandTotal` is `false` the adapter sets `grandTotalData` to `null`. AG Grid interprets a `null` on `params.success` as *explicitly remove the existing grand total row*, so forward it as `undefined` instead to keep the previously cached total intact.
:::

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/grand-total-row-grid.tsx)
- Backend source code of example grids available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/GrandTotalRowService.java)

import ShowSqlMonitor from './show-sql-monitor';
import LazyGrid from './lazy-grid';
import GrandTotalRowGrid from './grand-total-row-grid';
import GrandTotalRowAsyncGrid from './grand-total-row-async-grid';

<ShowSqlMonitor serviceUrls={['/docs/grand-total-row/getRows', '/docs/grand-total-row/supplySetFilterValues']}>
<LazyGrid>
<GrandTotalRowGrid></GrandTotalRowGrid>
</LazyGrid>
</ShowSqlMonitor>

## Getting Grand Total Data asynchronously
The grand total can be fetched on a separate endpoint so page loads don't wait for the aggregation. Use `queryBuilder.getGrandTotalData(request)` to compute it.

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/grand-total-row-async-grid.tsx)
- Backend source code of example grids available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/GrandTotalRowService.java)


<ShowSqlMonitor serviceUrls={['/docs/grand-total-row/async/getRows', '/docs/grand-total-row/async/supplySetFilterValues', '/docs/grand-total-row/async/getGrandTotalData']}>
<LazyGrid>
<GrandTotalRowAsyncGrid></GrandTotalRowAsyncGrid>
</LazyGrid>
</ShowSqlMonitor>
