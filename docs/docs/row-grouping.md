---
sidebar_position: 5
---

# Row Grouping
The Grid can group rows with equivalent cell values under shared parent rows.

If you want to make column available for grouping, you need to set the `enableRowGroup` parameter to `true` on `ColDef`,
otherwise grouping attempt on this column will result to runtime exception.

```java
ColDef priceColumn = ColDef.builder()
    .field("price")
    .enableRowGroup(true)
    .build();
```
