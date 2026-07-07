---
sidebar_position: 9
---


# Tree Data

To enable Tree Data, set `.treeData(true)` and configure the field mappings needed to traverse the hierarchy. The entity's ID attribute is passed as the second argument to `QueryBuilder.builder(entityClass, primaryField, ...)` (it is not a chained method).

| Method | Type | Required      | Description                                                                                                                                                                                                                                  |
| :--- | :--- |:--------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `treeData` | `boolean` | **Yes**       | Enables Tree Data mode.                                                                                                                                                                                                                      |
| `isServerSideGroupFieldName` | `String` | **Yes**       | Name of the virtual boolean field indicating if a row has children. Matches field used in [isServerSideGroup](https://www.ag-grid.com/react-data-grid/server-side-model-tree-data/#reference-serverSideRowModel-isServerSideGroup) callback. |
| `treeDataParentReferenceField` | `SingularAttribute<E, ?>` | *Conditional* | The parent-reference attribute (e.g. `Trade_.parentTrade`). **Required** if not using `treeDataParentIdField`.                                                                                                                                                          |
| `treeDataParentIdField` | `SingularAttribute<E, ?>` | *Conditional* | The raw parent-ID attribute. **Required** if not using `treeDataParentReferenceField`.                                                                                                                                                  |
| `treeDataChildrenField` | `PluralAttribute<E, ?, ?>` | No            | The child-collection attribute (e.g. `Trade_.childTrades`).                                                                                                                                                                                                          |
| `treeDataDataPathFieldName` | `SingularAttribute<E, String>` | No            | The attribute storing the path string. Use if you want to utilize standard tree filtering.                                                                                                                                          |
| `treeDataDataPathSeparator` | `String` | No            | The string separator used in your path. For example, in path `1/2/3`, this would be `/`.                                                                                                                                                     |

Example:

```java
this.queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager) // primary key attribute is the 2nd arg
    .colDefs(
        // colDefs
    )
    
    // turn on treeData
    .treeData(true)
    
    // name of field in which we return whether has children (required)
    .isServerSideGroupFieldName("hasChildren")
    
    // attribute that references the parent (provide one of them)
    .treeDataParentReferenceField(Trade_.parentTrade) // mapped parent entity
    // .treeDataParentIdField(Trade_.parentTradeId) // raw parent ID field
    
    // attribute which holds child collection (optional)
    .treeDataChildrenField(Trade_.childTrades)
    .build();
```

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/tree-data-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/TreeDataService.java)

import ShowSqlMonitor from './show-sql-monitor';
import TreeDataGrid from './tree-data-grid';
import TreeDataAggGrid from './tree-data-agg-grid';
import TreeDataChildCountGrid from './tree-data-child-count-grid';
import TreeDataFilteringGrid from './tree-data-filtering-grid';
import TreeDataFilteringAllGrid from './tree-data-filtering-all-grid';
import LazyGrid from './lazy-grid';

<ShowSqlMonitor serviceUrls={['/docs/tree-data/getRows']}>
<LazyGrid>
    <TreeDataGrid></TreeDataGrid>
</LazyGrid>
</ShowSqlMonitor>

## Providing Child Counts

:::warning
To make providing child counts work, you need to provide `treeDataDataPathFieldName` parameter.
:::

When showing child counts with Tree Data, the child count is a count of all descendants, including groups.

To receive child counts for groups, you need to set `getChildCount` param to `true` and provide property name `getChildCountFieldName` in which server returns the count.

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/tree-data-child-count-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/TreeDataService.java)

<ShowSqlMonitor serviceUrls={['/docs/tree-data/child-count/getRows']}>
<LazyGrid>
    <TreeDataChildCountGrid></TreeDataChildCountGrid>
</LazyGrid>
</ShowSqlMonitor>


## Aggregations on Tree Data

:::warning
To make aggregation on tree data work, you need to provide `treeDataDataPathFieldName` parameter.
:::

You can get aggregates on group-level nodes the same way as with regular grouping.

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/tree-data-agg-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/TreeDataService.java)


<ShowSqlMonitor serviceUrls={['/docs/tree-data/agg/getRows']}>
<LazyGrid>
<TreeDataAggGrid></TreeDataAggGrid>
</LazyGrid>
</ShowSqlMonitor>


## Filtering Tree Data

When filtering Tree Data in Server-Side Row Model, the adapter follows the standard AG Grid filtering logic.

A group will be included if:
1. it has any children that pass the filter, or
2. it has a parent that passes the filter, or
3. its own data passes the filter

:::warning
To make filtering of tree data work as expected, you need to provide `treeDataDataPathFieldName` parameter.
:::

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/tree-data-filtering-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/TreeDataService.java)

<ShowSqlMonitor serviceUrls={['/docs/tree-data/filtering/getRows']}>
    <TreeDataFilteringGrid></TreeDataFilteringGrid>
</ShowSqlMonitor>

### Ignore Filters when Aggregating Values

When using Tree Data and filters, the aggregates are only calculated from the rows which pass the filter. 
This can be changed by enabling the queryBuilder option `suppressAggFilteredOnly`.
