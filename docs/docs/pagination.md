---
sidebar_position: 8
---

# Pagination
When using pagination for the grid, you need to provide the total row count through a separate request to the backend. 

Only make this request when grid state changes in a way that affects the total count (like filter changes).
The count is for the level the request loads: for a grouped grid at the root, that is the number of root groups,
which is what the grid splits into pages.

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

It is the same count `countRows` returns, for the level the request loads: rows on a flat grid, the groups
of the first unexpanded group column when grouping, and the rows inside the group once every group is
expanded. Each group the grid expands loads its own level, so it gets that level's count.

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
