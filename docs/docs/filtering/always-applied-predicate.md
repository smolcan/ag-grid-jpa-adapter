---
sidebar_position: 8
---

# Always Applied Predicate

A predicate added to every query the adapter builds, no matter what the client sends. Use it for rules the
user must not be able to lift, such as tenant isolation.

```java
QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
    .colDefs(/* ... */)
    .alwaysAppliedPredicate((cb, root) -> cb.equal(root.get(Trade_.tenantId), tenantProvider.currentTenant()))
    .build();
```

It is present in every `WHERE` clause the adapter builds, so it is always `AND`ed with the filtering that
comes with the request.

## Always Applied Predicate Example

This grid is built with `.alwaysAppliedPredicate((cb, root) -> cb.isTrue(root.get(Trade_.isSold)))`, so it
can only ever return sold trades.

- the `Is Sold` set filter only offers `true`, because its values are read through the same predicate
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/filtering/always-applied-predicate-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/AlwaysAppliedPredicateService.java)

import ShowSqlMonitor from './../show-sql-monitor';
import AlwaysAppliedPredicateGrid from './always-applied-predicate-grid';
import LazyGrid from '../lazy-grid';

<ShowSqlMonitor serviceUrls={['/docs/filtering/always-applied-predicate/getRows', '/docs/filtering/always-applied-predicate/supplySetFilterValues']}>
<LazyGrid>
<AlwaysAppliedPredicateGrid></AlwaysAppliedPredicateGrid>
</LazyGrid>
</ShowSqlMonitor>
