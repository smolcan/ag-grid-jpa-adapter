# **JPA Adapter for AG Grid Server-Side Mode**

A lightweight Maven library for integrating **[AG Grid Server-Side Mode](https://ag-grid.com/angular-data-grid/server-side-model/)** with backend applications using **JPA**. This solution simplifies querying mapped entities for AG Grid and supports advanced server-side operations, including sorting, filtering, pagination, row grouping, and pivoting.

## **⚠️ Disclaimer: Active Development**
This project is currently in active development.
It is not fully tested and may contain bugs or incomplete features.
Development will continue for the next 12 months, and significant changes or breaking updates may occur during this time.

---

## **1. Key Features** 🚀

- **Sorting**: Sort data directly at the database level.
- **Filtering**: Apply built-in or custom filters to your data.
- **Pagination**: Efficiently handle large datasets with server-side pagination.
- **Row Grouping**: Group rows seamlessly using JPA queries.
- **Pivoting**: Execute pivot operations on your grid data <b>(not implemented yet)</b>.

---

## **2. Requirements** 🛠️

- **Java**: Version **11** or higher.
- **JPA**: Version **3.1.0** (or compatible).
- **AG Grid**: Server-Side Row Model integration.

---

## **3. Installation** 📦
This library is currently **not published yet** to Maven Central. To use it in your project, you need to manually copy the files into your project.
It will be published after the version is properly tested and stable.


## **4. How it works** 📘
1. The AG Grid frontend sends a ServerSideGetRowsRequest object to the backend.
2. The QueryBuilder processes the request:
    - Builds a dynamic JPA Criteria query based on the request parameters.
    - Executes the query to fetch and process the requested data.
3. The backend returns a LoadSuccessParams object to the frontend

## **5. Usage** 📘
**Initialize the QueryBuilder** <br/>
The QueryBuilder class dynamically generates and executes queries based on the ServerSideGetRowsRequest from AG Grid and returns a LoadSuccessParams object. <br/><br/>
**Here’s an example of a service class using the QueryBuilder**
```java
import com.aggrid.jpa.adapter.query.QueryBuilder;
import com.aggrid.jpa.adapter.response.LoadSuccessParams;
import com.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TradeService {
    
    private final EntityManager entityManager;
    private final QueryBuilder<Trade> queryBuilder;
    
    @Autowired
    public TradeService(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.queryBuilder = new QueryBuilder<>(Trade.class, entityManager);
    }
    
    public LoadSuccessParams getRows(ServerSideGetRowsRequest request) {
        return this.queryBuilder.getRows(request);
    }
}
```
