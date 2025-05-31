---
sidebar_position: 3
---

# Sorting

Sorting can be either **ascending** or **descending**, represented by the enum [SortType](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/request/SortType.java).

## Disabling Sorting

To prevent sorting on a column, set the `sortable` property in the column definition to `false`:

```java
ColDef priceColumn = ColDef.builder()
    .field("price")
    .sortable(false)
    .build();
```


## Grid using server side sorting

- sorting `Trade ID` works
- sorting `Product` is turned off
- sorting `Portfolio` is turned on in client, but turned off in server: `Server throws exception`

import SortingGrid from './sorting-grid';

<SortingGrid></SortingGrid>
