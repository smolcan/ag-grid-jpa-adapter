# **JPA Adapter for AG Grid Server-Side Mode**

A lightweight Maven library for integrating **AG Grid Server-Side Mode** with backend applications using **JPA**. This solution simplifies querying mapped entities for AG Grid and supports advanced server-side operations, including sorting, filtering, pagination, row grouping, and pivoting.

---

## **1. Key Features** 🚀

- **Sorting**: Sort data directly at the database level.
- **Filtering**: Apply built-in or custom filters to your data.
- **Pagination**: Efficiently handle large datasets with server-side pagination.
- **Row Grouping**: Group rows seamlessly using JPA queries.
- **Pivoting**: Execute pivot operations on your grid data.

---

## **2. Requirements** 🛠️

- **Java**: Version **11** or higher.
- **JPA**: Version **3.1.0** (or compatible).
- **AG Grid**: Server-Side Row Model integration.

---

## **3. Installation** 📦

Add the following Maven dependency to your project:

```xml
<dependency>
    <groupId>sk.smolcan</groupId>
    <artifactId>ag-grid-jpa-adapter</artifactId>
    <version>1.0.0</version>
</dependency>
```

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
