---
sidebar_position: 9
---


# Tree Data

To enable Tree Data, set `.treeData(true)` and configure the field mappings needed to traverse the hierarchy.

| Method | Type | Required      | Description                                                                                                                                                                                                                                  |
| :--- | :--- |:--------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `treeData` | `boolean` | **Yes**       | Enables Tree Data mode.                                                                                                                                                                                                                      |
| `primaryFieldName` | `String` | **Yes**       | Name of the entity's unique ID field. Matches field used in [getServerSideGroupKey](https://www.ag-grid.com/react-data-grid/server-side-model-tree-data/#reference-serverSideRowModel-getServerSideGroupKey) callback.                       |
| `isServerSideGroupFieldName` | `String` | **Yes**       | Name of the virtual boolean field indicating if a row has children. Matches field used in [isServerSideGroup](https://www.ag-grid.com/react-data-grid/server-side-model-tree-data/#reference-serverSideRowModel-isServerSideGroup) callback. |
| `treeDataParentReferenceField` | `String` | *Conditional* | Name of the parent entity field. **Required** if not using `treeDataParentIdField`.                                                                                                                                                          |
| `treeDataParentIdField` | `String` | *Conditional* | Name of the raw parent ID column. **Required** if not using `treeDataParentReferenceField`.                                                                                                                                                  |
| `treeDataChildrenField` | `String` | No            | Name of the child collection field.                                                                                                                                                                                                          |
| `treeDataDataPathFieldName` | `String` | No            | The name of the column storing the path string. Use if you want to utilize standard tree filtering.                                                                                                                                          |
| `treeDataDataPathSeparator` | `String` | No            | The string separator used in your path. For example, in path `1/2/3`, this would be `/`.                                                                                                                                                     |

Example:

```java
this.queryBuilder = QueryBuilder.builder(Entity.class, entityManager)
    .colDefs(
        // colDefs
    )
    
    // turn on treeData
    .treeData(true)
    
    // name of Id field in your entity (required)
    .primaryFieldName("tradeId")
    
    // name of field in which we return whether has children (required)
    .isServerSideGroupFieldName("hasChildren")
    
    // name of field that references to father (provide one of them)
    .treeDataParentReferenceField("parentTrade") // mapped father entity
    // .treeDataParentIdField("parentTradeId") // just ID
    
    // name of field which holds child collection (optional)
    .treeDataChildrenField("childTrades")
    .build();
```

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/tree-data-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/TreeDataService.java)

import GridLoadingMessage from './grid-loading-message';
import TreeDataGrid from './tree-data-grid';
import TreeDataAggGrid from './tree-data-agg-grid';
import TreeDataChildCountGrid from './tree-data-child-count-grid';
import TreeDataFilteringGrid from './tree-data-filtering-grid';
import TreeDataFilteringAllGrid from './tree-data-filtering-all-grid';

<GridLoadingMessage serviceUrls={['/docs/tree-data/getRows']}>
    <TreeDataGrid></TreeDataGrid>
</GridLoadingMessage>

## Providing Child Counts

:::warning
To make providing child counts work, you need to provide `treeDataDataPathFieldName` parameter.
:::

When showing child counts with Tree Data, the child count is a count of all descendants, including groups.

To receive child counts for groups, you need to set `getChildCount` param to `true` and provide property name `getChildCountFieldName` in which server returns the count.

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/tree-data-child-count-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/TreeDataService.java)

<GridLoadingMessage serviceUrls={['/docs/tree-data/child-count/getRows']}>
    <TreeDataChildCountGrid></TreeDataChildCountGrid>
</GridLoadingMessage>


## Aggregations on Tree Data

:::warning
To make aggregation on tree data work, you need to provide `treeDataDataPathFieldName` parameter.
:::

You can get aggregates on group-level nodes the same way as with regular grouping.

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/tree-data-agg-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/TreeDataService.java)


<GridLoadingMessage serviceUrls={['/docs/tree-data/agg/getRows']}>
<TreeDataAggGrid></TreeDataAggGrid>
</GridLoadingMessage>


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

<GridLoadingMessage serviceUrls={['/docs/tree-data/filtering/getRows']}>
    <TreeDataFilteringGrid></TreeDataFilteringGrid>
</GridLoadingMessage>

### Filtering with non-column filters

Tree filtering also works with non-column filter types: Advanced filters, External filters and Quick filters.

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/tree-data-filtering-all-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/TreeDataService.java)

<GridLoadingMessage serviceUrls={['/docs/tree-data/filtering/all/getRows']}>
    <TreeDataFilteringAllGrid></TreeDataFilteringAllGrid>
</GridLoadingMessage>

### Ignore Filters when Aggregating Values

When using Tree Data and filters, the aggregates are only calculated from the rows which pass the filter. 
This can be changed by enabling the queryBuilder option `suppressAggFilteredOnly`.
