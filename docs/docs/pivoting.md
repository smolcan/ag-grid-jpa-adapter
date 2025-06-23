---
sidebar_position: 7
---

# Pivoting
Pivoting breaks down data in an additional dimension.

import PivotingGrid from './pivoting-grid';
import GridLoadingMessage from './grid-loading-message';

<GridLoadingMessage>
    <PivotingGrid></PivotingGrid>
</GridLoadingMessage>

## Enabling Pivoting
To make column available for pivoting, set the `enablePivot` parameter to `true` on `ColDef`,
otherwise pivoting attempt on this column will result to runtime exception.

```java
ColDef priceColumn = ColDef.builder()
    .field("price")
    .enablePivot(true)
    .build();
```

## Best Practices - Limiting Column Generation
When pivoting, changes in data, aggregation or pivot columns can cause the number of generated columns to scale exponentially.
To prevent this from happening, you can set the `pivotMaxGeneratedColumns` option on `QueryBuilder`.

When the grid generates a number of pivot columns exceeding this value, it halts column generation and throws 
the [`OnPivotMaxColumnsExceededException`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/exceptions/OnPivotMaxColumnsExceededException.java).

```java
this.queryBuilder = QueryBuilder.builder(Entity.class, entityManager)
                .colDefs(
                        // colDefs
                )
                .pivotMaxGeneratedColumns(1000)
                .build();
```
