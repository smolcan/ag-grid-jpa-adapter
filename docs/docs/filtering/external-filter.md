---
sidebar_position: 6
---

# External Filter

:::warning
This feature is officially not supported for server-side row model in Official Ag-Grid documentation 
:::

## External Filter Configuration

You must configure the QueryBuilder with two key parameters: `isExternalFilterPresent` and `doesExternalFilterPass`

```java
QueryBuilder.builder(Trade.class, entityManager)
    .colDefs(/* ... */)
    .isExternalFilterPresent(true)
    .doesExternalFilterPass((cb, root, filterValue) -> {
        // logic to convert filterValue to Predicate
        if (Boolean.TRUE.equals(filterValue)) {
            return cb.greaterThan(root.get("currentValue"), 1000);
        }
        // won't apply
        return null;
    })
    .build();
```

In your frontend, you need to add the value to payload like this
```javascript
fetch('/api/rows', {
  method: 'POST',
  body: JSON.stringify({
    ...params.request,
    externalFilter: myExternalFilterState // Your custom filter value
  }),
  ...
})
```

## External Filer Example

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/pagination-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/PaginationService.java)

import GridLoadingMessage from './../grid-loading-message';
import ExternalFilterGrid from './external-filter-grid';

<GridLoadingMessage>
<ExternalFilterGrid></ExternalFilterGrid>
</GridLoadingMessage>