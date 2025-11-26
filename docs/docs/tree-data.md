---
sidebar_position: 9
---


# Tree Data

To enable Tree Data, set `.treeData(true)` and configure the field mappings needed to traverse the hierarchy.

| Method | Type | Required | Description                                                                                                                                                                                                                                  |
| :--- | :--- | :--- |:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `treeData` | `boolean` | **Yes** | Enables Tree Data mode.                                                                                                                                                                                                                      |
| `primaryFieldName` | `String` | **Yes** | Name of the entity's unique ID field. Matches field used in [getServerSideGroupKey](https://www.ag-grid.com/react-data-grid/server-side-model-tree-data/#reference-serverSideRowModel-getServerSideGroupKey) callback.                       |
| `isServerSideGroupFieldName` | `String` | **Yes** | Name of the virtual boolean field indicating if a row has children. Matches field used in [isServerSideGroup](https://www.ag-grid.com/react-data-grid/server-side-model-tree-data/#reference-serverSideRowModel-isServerSideGroup) callback. |
| `treeDataParentReferenceField` | `String` | *Conditional* | Name of the parent entity field. **Required** if not using `treeDataParentIdField`.                                                                                                                                                          |
| `treeDataParentIdField` | `String` | *Conditional* | Name of the raw parent ID column. **Required** if not using `treeDataParentReferenceField`.                                                                                                                                                  |
| `treeDataChildrenField` | `String` | No | Name of the child collection field.                                                                                                                                                                                                          |

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

Source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/TreeDataService.java)

import GridLoadingMessage from './grid-loading-message';
import TreeDataGrid from './tree-data-grid';

<GridLoadingMessage>
    <TreeDataGrid></TreeDataGrid>
</GridLoadingMessage>

## Filtering Tree Data

WIP, WILL BE IMPLEMENTED IN THE FUTURE