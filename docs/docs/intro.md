---
slug: /
sidebar_position: 1
---
# Quick Start
## Introduction
A lightweight Maven library for integrating **[AG Grid Server-Side Mode](https://ag-grid.com/angular-data-grid/server-side-model/)** with backend applications using **JPA**. 
This solution simplifies querying mapped entities for AG Grid and supports advanced server-side operations, including sorting, filtering, pagination, row grouping, and pivoting.

**⚠️ Disclaimer: Active Development**
This project is currently in active development.
It is not fully tested and may contain bugs or incomplete features.
Development will continue for the next 12 months, and significant changes or breaking updates may occur during this time.


## Installation

**Available via maven dependency**
```xml
<dependency>
    <groupId>io.github.smolcan</groupId>
    <artifactId>ag-grid-jpa-adapter</artifactId>
    <version>0.0.29</version>
</dependency>
```
**Requirements 🛠️**
- **Java**: Version **11** or higher.
- **JPA**: Version **3.1.0**.

## Using AG Grid JPA Adapter
When enabling 'serverSide' row model type in your AG Grid, you must provide [datasource](https://ag-grid.com/react-data-grid/server-side-model-datasource/),
which is used to fetch rows for the grid, such as:
``` javascript title="AG Grid example of datasource"
const createDatasource = server => {
    return {
        // called by the grid when more rows are required
        getRows: params => {

            // get data for request from server
            const response = server.getData(params.request);

            if (response.success) {
                // supply rows for requested block to grid
                params.success({
                    rowData: response.rows
                });
            } else {
                // inform grid request failed
                params.fail();
            }
        }
    };
}
```

**AG Grid JPA Adapter** can process the AG Grid’s request and return corresponding data using JPA abstraction, 
which avoids the use of native queries, 
ensures compatibility with multiple databases, 
and promotes maintainable and scalable code.

The only entrypoint you interact with is [QueryBuilder](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/query/QueryBuilder.java),
and it's method **getRows**, which processes a [ServerSideGetRowsRequest](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/request/ServerSideGetRowsRequest.java) object,
builds the query using **JPA Criteria API**, executes it and returns [LoadSuccessParams](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/response/LoadSuccessParams.java) object,
which you can directly return to the grid.
```java title="Java getRows method using QueryBuilder"
public class Service {
    private final QueryBuilder<Entity> queryBuilder;
    public Service() {
        // ...query builder initialization in class constructor
        this.queryBuilder = ....
    }
    
    public LoadSuccessParams getRows(ServerSideGetRowsRequest request) {
        return this.queryBuilder.getRows(request);
    }
}
```