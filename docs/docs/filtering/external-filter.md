---
sidebar_position: 6
---

# External Filter

:::info
AG Grid's frontend does not natively wire external filters in Server-Side Row Model. This adapter implements the equivalent logic server-side: the filter value is passed in the request body and converted to a JPA predicate.
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

## External Filter Example

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/filtering/external-filter-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/ExternalFilterService.java)

import ShowSqlMonitor from './../show-sql-monitor';
import ExternalFilterGrid from './external-filter-grid';
import LazyGrid from '../lazy-grid';

<ShowSqlMonitor serviceUrls={['/docs/filtering/external-filter/getRows']}>
<LazyGrid>
<ExternalFilterGrid></ExternalFilterGrid>
</LazyGrid>
</ShowSqlMonitor>