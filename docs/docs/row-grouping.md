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

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/row-grouping-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/RowGroupingService.java)

import RowGroupingGrid from './row-grouping-grid';
import GridLoadingMessage from './grid-loading-message';

<GridLoadingMessage>
    <RowGroupingGrid></RowGroupingGrid>
</GridLoadingMessage>