---
sidebar_position: 6
---

# Aggregation
Apply provided functions to values to calculate group values in the grid.

## Enabling Aggregation
To make column available for aggregation, set the `enableValue` parameter to `true` on `ColDef`,
otherwise aggregation attempt on this column will result to runtime exception.

To limit which aggregation functions ([AggregationFunction](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/request/AggregationFunction.java)) 
can be used on the column, provide array of allowed functions (otherwise all the aggregation functions are allowed.)

```java
ColDef priceColumn = ColDef.builder()
    .field("price")
    .enableValue(true)
    .allowedAggFuncs(AggregationFunction.avg, AggregationFunction.count)
    .build();
```


**⚠️ Disclaimer**
Currently aggregation functions `first` and `last` are not supported in JPA. Using these functions will result in runtime exception.
Supported functions are: `avg`, `sum`, `min`, `max`, `count`.
