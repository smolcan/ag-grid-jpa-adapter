---
slug: /
sidebar_position: 1
---
# Quick Start
## Introduction
A lightweight Maven library for integrating **[AG Grid Server-Side Mode](https://ag-grid.com/angular-data-grid/server-side-model/)** with backend applications using **JPA**. 
This solution simplifies querying mapped entities for AG Grid and supports advanced server-side operations, including sorting, filtering, pagination, row grouping, and pivoting.

:::warning Active Development
This project is under active development. Breaking changes may occur between versions. Check the [GitHub releases](https://github.com/smolcan/ag-grid-jpa-adapter/releases) before upgrading.
:::


## Installation

**Available via maven dependency**

<a href="https://central.sonatype.com/artifact/io.github.smolcan/ag-grid-jpa-adapter" target="_blank"><img src="https://img.shields.io/maven-central/v/io.github.smolcan/ag-grid-jpa-adapter?strategy=highestVersion&style=flat" alt="Version"/></a>

```xml
<dependency>
    <groupId>io.github.smolcan</groupId>
    <artifactId>ag-grid-jpa-adapter</artifactId>
    <version>${ag-grid-jpa-adapter.version}</version>
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
and its method **getRows**, which processes a [ServerSideGetRowsRequest](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/request/ServerSideGetRowsRequest.java) object,
builds the query using **JPA Criteria API**, executes it and returns a [LoadSuccessParams](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/response/LoadSuccessParams.java) object
which you can return directly to the grid.

## Complete Example

**1. Your JPA entity:**

```java
@Entity
@Table(name = "trade")
public class Trade {
    @Id
    private Long id;
    private String product;
    private String portfolio;
    private BigDecimal currentValue;
    // getters / setters
}
```

**2. Build a `QueryBuilder` once (e.g. in a Spring `@Service`):**

```java
@Service
public class TradeService {

    private final QueryBuilder<Trade> queryBuilder;

    public TradeService(EntityManager entityManager) {
        this.queryBuilder = QueryBuilder.builder(Trade.class, entityManager)
            .colDefs(
                ColDef.builder().field("id").sortable(true).build(),
                ColDef.builder().field("product").filter(new AgTextColumnFilter()).build(),
                ColDef.builder().field("portfolio").filter(new AgSetColumnFilter()).build(),
                ColDef.builder()
                    .field("currentValue")
                    .filter(new AgNumberColumnFilter())
                    .enableValue(true)
                    .enableRowGroup(true)
                    .build()
            )
            .build();
    }

    public LoadSuccessParams getRows(ServerSideGetRowsRequest request) {
        return this.queryBuilder.getRows(request);
    }
}
```

**3. Expose an endpoint:**

```java
@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;

    @PostMapping("/getRows")
    public LoadSuccessParams getRows(@RequestBody ServerSideGetRowsRequest request) {
        return tradeService.getRows(request);
    }
}
```

**4. Point AG Grid at your endpoint:**

```javascript
const datasource = {
    getRows: (params) => {
        fetch('/api/trades/getRows', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(params.request),
        })
        .then(res => res.json())
        .then(data => params.success({ rowData: data.rowData, rowCount: data.rowCount }))
        .catch(() => params.fail());
    }
};
gridApi.setGridOption('serverSideDatasource', datasource);
```