---
sidebar_position: 5
---

# Pagination
For [pagination](https://ag-grid.com/react-data-grid/server-side-model-pagination/) we receive these properties in [ServerSideGetRowsRequest](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/request/ServerSideGetRowsRequest.java):
```java title="Pagination information in ServerSideGetRowsRequest"
public class ServerSideGetRowsRequest {
    // ... other params

    // First row requested or undefined for all rows. 
    private int startRow;
    // Index after the last row required row or undefined for all rows.
    private int endRow;
    
    // ... other params
}
```

In [QueryBuilder](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/query/QueryBuilder.java), pagination is implemented
in method **limitOffset**
```java title="Limit offset method in QueryBuilder"
protected void limitOffset(TypedQuery<Tuple> typedQuery, ServerSideGetRowsRequest request) {
    typedQuery.setFirstResult(request.getStartRow());
    typedQuery.setMaxResults(request.getEndRow() - request.getStartRow() + 1);
}
``` 