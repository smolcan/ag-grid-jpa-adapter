---
sidebar_position: 3
---

# Filtering
[Filtering](https://ag-grid.com/angular-data-grid/server-side-model-filtering/) in AG Grid JPA Adapter supports 
[Column filtering](https://ag-grid.com/angular-data-grid/filtering/) and 
[Advanced filtering](https://ag-grid.com/angular-data-grid/filter-advanced/).

In [ServerSideGetRowsRequest](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/github/smolcan/aggrid/jpa/adapter/request/ServerSideGetRowsRequest.java)
we receive filter model as:
```java title="Filter model in ServerSideGetRowsRequest"
private Map<String, Object> filterModel;
```
We further analyze this filter model and decide, what kind of filter we received.

Filtering is implemented in [QueryBuilder](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/github/smolcan/aggrid/jpa/adapter/query/QueryBuilder.java)'s **where** method.
If different behaviour is needed, this method can be overwritten.