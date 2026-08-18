---
sidebar_position: 8
---

# Pagination
When using pagination for the grid, you need to provide the total row count through a separate request to the backend. 

Only make this request when grid state changes in a way that affects the total count (like filter changes).
For grids with grouping enabled, only root groups are counted. 

Use the `queryBuilder.countRows(request)` method to retrieve this count

- try to group by `Portfolio` column and see how counting groups work
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/pagination-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/PaginationService.java)

import PaginationGrid from './pagination-grid';
import ShowSqlMonitor from './show-sql-monitor';
import LazyGrid from './lazy-grid';

<ShowSqlMonitor serviceUrls={['/docs/pagination/getRows', '/docs/pagination/countRows']}>
<LazyGrid>
    <PaginationGrid></PaginationGrid>
</LazyGrid>
</ShowSqlMonitor>

## Row count in the response

Instead of exposing a second endpoint for `countRows`, set `includeRowCountInLoadSuccessParams` and the
count comes back in the same response, in `LoadSuccessParams.rowCount`.

```java
QueryBuilder<Entity, Long, Void> queryBuilder = QueryBuilder.builder(Entity.class, Entity_.id, entityManager)
    .colDefs(...)
    .includeRowCountInLoadSuccessParams(true)
    .build();
```

The count follows the same rules as `countRows`: rows on a flat grid, root groups when grouping, and the
rows inside the expanded group when `paginateChildRows` is on.

:::info Cost
Every `getRows` call then issues a second query for the count. If your grid only needs the count when the
filter changes, a separate endpoint called on those changes stays cheaper.
:::

- the grid above calls two endpoints, this one calls only `getRows` — watch the SQL monitor
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/row-count-in-response-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/PaginationService.java)

import RowCountInResponseGrid from './row-count-in-response-grid';

<ShowSqlMonitor serviceUrls={['/docs/pagination/row-count-in-response/getRows']}>
<LazyGrid>
    <RowCountInResponseGrid></RowCountInResponseGrid>
</LazyGrid>
</ShowSqlMonitor>


## Paginate child rows
Set `paginateChildRows=true` in `QueryBuilder` to maintain exact page size. This makes `queryBuilder.countRows(request)` 
count rows within expanded groups rather than number of root groups.

```java
QueryBuilder<Entity, Long, Void> queryBuilder = QueryBuilder.builder(Entity.class, Entity_.id, entityManager)
    .colDefs(...)
    .paginateChildRows(true)
    .build();
```

Note: When expanding groups in the frontend and `paginateChildRows` is set to `true`, you should also call count rows.

- try to group by `Portfolio` column and see how counting groups work
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/paginate-child-rows-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/PaginationService.java)


import PaginateChildRowsGrid from './paginate-child-rows-grid';

<ShowSqlMonitor serviceUrls={['/docs/pagination/paginate-child-rows/getRows', '/docs/pagination/paginate-child-rows/countRows']}>
<LazyGrid>
    <PaginateChildRowsGrid></PaginateChildRowsGrid>
</LazyGrid>
</ShowSqlMonitor>
