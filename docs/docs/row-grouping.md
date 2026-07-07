---
sidebar_position: 5
---

# Row Grouping
The Grid can group rows with equivalent cell values under shared parent rows.

If you want to make a column available for grouping, enable `enableRowGroup` on its `ColDef`,
otherwise a grouping attempt on this column will result in a runtime exception.

`enableRowGroup` takes two arguments: `true`, and a converter `Function<String, T>` from the group key
to the column's Java type `T`. AG Grid sends group keys as strings, so the adapter uses this converter
to turn the key back into the column type when building the query (e.g. a `String` column uses `s -> s`,
a number column `BigDecimal::new` / `Integer::valueOf`, a date column `LocalDate::parse`).

```java
var priceColumn = ColDef.builder(Entity_.price)
    .enableRowGroup(true, BigDecimal::new)
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
this.queryBuilder = QueryBuilder.builder(Entity.class, Entity_.id, entityManager)
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