---
sidebar_position: 3
---

# Grouping
We receive information about [Row grouping](https://ag-grid.com/react-data-grid/server-side-model-grouping/) in [ServerSideGetRowsRequest](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/request/ServerSideGetRowsRequest.java) in these two fields:
```java title="Grouping information in ServerSideGetRowsRequest"
public class ServerSideGetRowsRequest {
    // ... other params

    // Columns that are currently row grouped. 
    private List<ColumnVO> rowGroupCols = Collections.emptyList();
    // What groups the user is viewing.
    private List<String> groupKeys = Collections.emptyList();
    
    // ... other params
}
```

**rowGroupCols** tells us which columns are grouped and
**groupKeys** tells us about expanded groups and their keys.

In [QueryBuilder](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/query/QueryBuilder.java), this functionality
is implemented in method **groupBy** for grouping data and in method **where** to filter data of expanded group.



## Grouping Example
**Grid with grouping and all groups are collapsed:**
![Grid with grouping and all groups are collapsed](/img/group_img_1.png)
```JSON title="Payload when all groups are collapsed"
{
  "rowGroupCols": [
    {
      "id": "product",
      "displayName": "Product",
      "field": "product"
    },
    {
      "id": "portfolio",
      "displayName": "Portfolio",
      "field": "portfolio"
    }
  ],
  "groupKeys": []
}
```
```SQL title="Generated SQL select with all collapsed groups" 
select
    t.product 
from
    trade t
group by
    t.product 
offset
    0 rows 
fetch
    first 101 rows only
```

**Expanded first group:**
![Grid with grouping and expanded first group](/img/group_img_2.png)
```JSON title="Payload when expanded first group with key ProductA"
{
  "rowGroupCols": [
    {
      "id": "product",
      "displayName": "Product",
      "field": "product"
    },
    {
      "id": "portfolio",
      "displayName": "Portfolio",
      "field": "portfolio"
    }
  ],
  "groupKeys": [
    "ProductA"
  ]
}
```
```SQL title="Generated SQL select with one expanded group" 
select
    t.product,
    t.portfolio 
from
    trade t 
where
    t.product='ProductA' 
group by
    t.product,
    t.portfolio 
offset
    0 rows 
fetch
    first 101 rows only
```

**Expanded second group:**
![Grid with grouping and expanded second group](/img/group_img_3.png)
```JSON title="Payload when expanded also second group with key Portfolio1"
{
  "rowGroupCols": [
    {
      "id": "product",
      "displayName": "Product",
      "field": "product"
    },
    {
      "id": "portfolio",
      "displayName": "Portfolio",
      "field": "portfolio"
    }
  ],
  "groupKeys": [
    "ProductA",
    "Portfolio1"
  ]
}
```
```SQL title="Generated SQL select with all expanded groups (number of grouped cols is same as number of group keys)" 
select
    t.pl1,
    t.birth_date,
    t.previous_value,
    t.deal_type,
    t.submitter_deal_id,
    t.product,
    t.current_value,
    t.gain_dx,
    t.pl2,
    t.submitter_id,
    t.bid_type,
    t.is_sold,
    t.trade_id,
    t.x99_out,
    t.book,
    t.portfolio,
    t.sx_px,
    t.batch 
from
    trade t 
where
    t.product='ProductA'
    and t.portfolio='Portfolio1'
offset
    0 rows 
fetch
    first 101 rows only
```


## Aggregated fields
We receive aggregated fields in **valueCols** field.
```java title="Aggregation information in ServerSideGetRowsRequest"
public class ServerSideGetRowsRequest {
    // ... other params

    // Columns that have aggregations on them.
    private List<ColumnVO> valueCols = Collections.emptyList();    
    
    // ... other params
}
```

Possible aggregated functions:
- avg
- sum
- min
- max
- count
- first (not supported by JPA)
- last (not supported by JPA)
