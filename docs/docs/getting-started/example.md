---
sidebar_position: 3
---

# Example

Assuming you have table **Trade**
```sql title="trade table"
create table trade (
    product           varchar(255),
    portfolio         varchar(255),
    book              varchar(255),
    trade_id          serial
        primary key,
    submitter_id      integer,
    submitter_deal_id integer,
    deal_type         varchar(255),
    bid_type          varchar(255),
    current_value     numeric,
    previous_value    numeric,
    pl1               numeric,
    pl2               numeric,
    gain_dx           numeric,
    sx_px             numeric,
    x99_out           numeric,
    batch             integer,
    birth_date        date,
    is_sold           boolean
);
```

And mapped **entity** for this table

```java title="Trade entity"
@Entity
@Table(name = "trade")
@Getter @Setter
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    private Long tradeId;

    @Column(name = "product", length = 255)
    private String product;

    @Column(name = "portfolio", length = 255)
    private String portfolio;

    @Column(name = "book", length = 255)
    private String book;

    @Column(name = "submitter_id")
    private Integer submitterId;

    @Column(name = "submitter_deal_id")
    private Integer submitterDealId;

    @Column(name = "deal_type", length = 255)
    private String dealType;

    @Column(name = "bid_type", length = 255)
    private String bidType;

    @Column(name = "current_value")
    private BigDecimal currentValue;

    @Column(name = "previous_value")
    private BigDecimal previousValue;

    @Column(name = "pl1")
    private BigDecimal pl1;

    @Column(name = "pl2")
    private BigDecimal pl2;

    @Column(name = "gain_dx")
    private BigDecimal gainDx;

    @Column(name = "sx_px")
    private BigDecimal sxPx;

    @Column(name = "x99_out")
    private BigDecimal x99Out;

    @Column(name = "batch")
    private Integer batch;
    
    @Column(name = "birth_date")
    private LocalDate birthDate;
    
    @Column(name = "is_sold")
    private Boolean isSold;
}
```

## Initialize the [QueryBuilder](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/query/QueryBuilder.java)
To use the [QueryBuilder](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/query/QueryBuilder.java), 
you need to initialize it with your entity class and an **EntityManager**. 
The **getRows** method processes a [ServerSideGetRowsRequest](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/request/ServerSideGetRowsRequest.java) payload 
and returns the data formatted according to the requirements of the grid.


```java title="Example in Spring Boot app"
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
    
    /**
     * Fetches rows from the database based on the grid's request.
     *
     * @param request the ServerSideGetRowsRequest payload from the grid.
     * @return LoadSuccessParams containing the data formatted for the grid.
     */
    public LoadSuccessParams getRows(ServerSideGetRowsRequest request) {
        return this.queryBuilder.getRows(request);
    }
}
```
