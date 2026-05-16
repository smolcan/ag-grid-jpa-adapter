---
sidebar_position: 11
---

# Grand Total Row
A grand total row aggregates values across all rows in the grid and is displayed as an extra row at the top or bottom.

To enable it, set `grandTotalRow(true)` on the `QueryBuilder`. Without this flag the adapter will not compute the grand total even if the frontend requests it.

```java
QueryBuilder<Entity> queryBuilder = QueryBuilder.builder(Entity.class, entityManager)
    .colDefs(...)
    .grandTotalRow(true)
    .build();
```

## Providing Grand Total Data
The grand total row data is returned on `loadSuccessParams.grandTotalData` as part of the standard `getRows` response. The adapter only computes it when the request carries `needsGrandTotal=true`, so the frontend has to forward that hint:

```js
fetch(`${API_URL}/docs/grand-total-row/getRows`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        ...params.request,
        needsGrandTotal: params.needsGrandTotal,
    }),
})
```

When `needsGrandTotal` is `false` the adapter sets `grandTotalData` to `null`. AG Grid interprets a `null` on `params.success` as *explicitly remove the existing grand total row*, so forward it as `undefined` instead to keep the previously cached total intact:

```js
.then(data => {
    const successData = data.data;
    if (successData.grandTotalData === null) {
        successData.grandTotalData = undefined;
    }
    params.success(successData);
})
```

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
The grand total can be fetched on a separate endpoint so page loads don't wait for the aggregation. Expose it via `queryBuilder.getGrandTotalData(request)` and on the frontend push the result into the grid with `applyServerSideTransaction` using the exported `GRAND_TOTAL_ROW_ID` constant.

:::warning
Pass `addIndex: 0` on the add transaction when the rows response has no `rowCount` — otherwise AG Grid silently drops the grand total.
:::

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/grand-total-row-async-grid.tsx)
- Backend source code of example grids available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/GrandTotalRowService.java)


<ShowSqlMonitor serviceUrls={['/docs/grand-total-row/async/getRows', '/docs/grand-total-row/async/supplySetFilterValues', '/docs/grand-total-row/async/getGrandTotalData']}>
<LazyGrid>
<GrandTotalRowAsyncGrid></GrandTotalRowAsyncGrid>
</LazyGrid>
</ShowSqlMonitor>
