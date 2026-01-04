---
sidebar_position: 5
---

# Row Grouping
The Grid can group rows with equivalent cell values under shared parent rows.

If you want to make column available for grouping, you need to set the `enableRowGroup` parameter to `true` on `ColDef`,
otherwise grouping attempt on this column will result to runtime exception.

```java
ColDef priceColumn = ColDef.builder()
    .field("price")
    .enableRowGroup(true)
    .build();
```

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/row-grouping-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/RowGroupingService.java)

import RowGroupingGrid from './row-grouping-grid';
import RowGroupingChildCountGrid from './row-grouping-child-count-grid';
import ShowSqlMonitor from './show-sql-monitor';
import LazyGrid from './lazy-grid';


<ShowSqlMonitor serviceUrls={['/docs/row-grouping/getRows']}>
<LazyGrid>
    <RowGroupingGrid></RowGroupingGrid>
</LazyGrid>
</ShowSqlMonitor>

## Providing Child Counts

To enable child counts, you need to set `getChildCount` on query builder to `true`, 
and also provide name of the field where child count will be returned (this is the same field you will use in `getChildCount` callback in client).

```java
this.queryBuilder = QueryBuilder.builder(Entity.class, entityManager)
    .colDefs(
        // colDefs
    )
    .getChildCount(true)
    .getChildCountFieldName("childCount")
    .build();
```

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/row-grouping-child-count-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/RowGroupingService.java)

<ShowSqlMonitor serviceUrls={['/docs/row-grouping/child-count/getRows']}>
<LazyGrid>
    <RowGroupingChildCountGrid></RowGroupingChildCountGrid>
</LazyGrid>
</ShowSqlMonitor>