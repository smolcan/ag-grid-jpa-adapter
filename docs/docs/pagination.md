---
sidebar_position: 8
---

# Pagination
When using pagination for the grid, you need to provide the total row count through a separate request to the backend. 

Only make this request when grid state changes in a way that affects the total count (like filter changes).
For grids with grouping enabled, only root groups are counted. 

Use the `queryBuilder.countRows(request)` method to retrieve this count

- try to group by `Portfolio` column and see how counting groups work

import PaginationGrid from './pagination-grid';

<PaginationGrid></PaginationGrid>

## Paginate child rows
Set `paginateChildRows=true` in `QueryBuilder` to maintain exact page size. This makes `queryBuilder.countRows(request)` 
count rows within expanded groups rather than number of root groups.

```java
QueryBuilder<Entity> queryBuilder = QueryBuilder.builder(Entity.class, entityManager)
    .colDefs(...)
    .paginateChildRows(true)
    .build();
```

Note: When expanding groups in the frontend and `paginateChildRows` is set to `true`, you should also call count rows.

- try to group by `Portfolio` column and see how counting groups work

import PaginateChildRowsGrid from './paginate-child-rows-grid';


<PaginateChildRowsGrid></PaginateChildRowsGrid>