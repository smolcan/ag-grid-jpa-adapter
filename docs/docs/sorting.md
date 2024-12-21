---
sidebar_position: 2
---

# Sorting
[Sorting](https://ag-grid.com/angular-data-grid/server-side-model-sorting/) is performed according to received **sortModel** in [ServerSideGetRowsRequest](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/request/ServerSideGetRowsRequest.java)
```javascript title="Sort model example from AG Grid documentation"
{
    sortModel: [
        { colId: 'country', sort: 'asc' },
        { colId: 'year', sort: 'desc' },
    ]
}
```

Sorting is implemented in [QueryBuilder](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/query/QueryBuilder.java)'s **orderBy** method,
which copies the default sorting behaviour from the [official AG Grid documentation](https://ag-grid.com/angular-data-grid/server-side-model-sorting/).
If different behaviour is needed, this method can be overwritten.