---
sidebar_position: 2
---

# Columns

In order to define which columns should be returned to the client, we need to use [ColDefs](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/column/ColDef.java) objects. 
Each column that we want to include in the AG Grid response must be explicitly defined in the [ColDefs](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/column/ColDef.java).

## Defining Columns

Each column is defined using a [`ColDef`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/column/ColDef.java) object.

| Property                 | Type                                                                                                                                         | Default                                                                                                                                                                            | Description                                                                                                                                                                                                                                                                                                                                    |
|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`field`** *(required)* | `SingularAttribute` / `FieldPath`                                                                                                                                     | —                                                                                                                                                                                  | The entity field, passed to `ColDef.builder(...)` as a JPA metamodel attribute (e.g. `Trade_.price`) or a `FieldPath` for nested paths.                                                                                                                                                                                                                                                                                                                  |
| **`sortable`**           | `boolean`                                                                                                                                    | `true`                                                                                                                                                                             | Enables or disables sorting.                                                                                                                                                                                                                                                                                                                   |
| **`filter`**             | [`IFilter`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/IFilter.java) | — | Defines the filter type (no default — a column has no filter unless one is set). <br/> Supports: <br/> ✅ Custom `IFilter` implementations <br/> ✅ Built-in filters (e.g., [`AgNumberColumnFilter`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/simple/AgNumberColumnFilter.java)) <br/> ⚠️ Omit `.filter(...)` to leave the column without a filter |
| **`enableValue`**        | `boolean`                                                                                                                                    | `false`                                                                                                                                                                            | Set to `true` if you want to be able to aggregate by this column.                                                                                                                                                                                                                                                                              |
| **`enableRowGroup`**     | `boolean`                                                                                                                                    | `false`                                                                                                                                                                            | Set to `true` if you want to be able to row group by this column.                                                                                                                                                                                                                                                                              |
| **`enablePivot`**     | `boolean`                                                                                                                                    | `false`                                                                                                                                                                            | Set to `true` if you want to be able to pivot by this column.                                                                                                                                                                                                                                                                                  |
| **`allowedAggFuncs`**    | `Set<AggregationFunction>`                                                                                                                   | All available                                                                                                                                                                      | Defines allowed aggregation functions.                                                                                                                                                                                                                                                                                                         |


## Example Usage

```java
var priceColumn = ColDef.builder(Entity_.price)
    .sortable(true)
    .filter(AgSetColumnFilter.forNumber())
    .allowedAggFuncs(AggregationFunction.avg, AggregationFunction.count)
    .build();

// no .filter(...) -> this column has no filter
var nameColumn = ColDef.builder(Entity_.name)
    .sortable(false)
    .build();

QueryBuilder<Entity, Long, Void> queryBuilder = QueryBuilder.builder(Entity.class, Entity_.id, entityManager)
    .colDefs(priceColumn, nameColumn)
    .build();
```

## Dot notation

The adapter fully supports columns on nested/related entities. The path is built with [`FieldPath`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/column/FieldPath.java), chaining `.to(...)` through the mapped relationships. The resulting column name in the AG Grid response still uses dot notation (e.g. `submitter.id`).

```java
// submitter.id
ColDef.builder(FieldPath.of(Trade_.submitter).to(Submitter_.id)).build()

// parentTrade.parentTrade.tradeId
ColDef.builder(FieldPath.of(Trade_.parentTrade).to(Trade_.parentTrade).to(Trade_.tradeId)).build()
```

:::info JPA Requirement
Each hop must correspond to a mapped relationship (e.g., `@ManyToOne`, `@OneToOne`) within your JPA Entity.
It uses `LEFT JOINS` to join the table.
:::

- `Trade Id` is from main table
- `Submitter Id` uses `FieldPath.of(Trade_.submitter).to(Submitter_.id)`, referencing the `submitter` entity
- `Submitter Deal Id` uses `FieldPath.of(Trade_.submitterDeal).to(SubmitterDeal_.id)`, referencing the `submitterDeal` entity
- `Parent Trade Id` uses `FieldPath.of(Trade_.parentTrade).to(Trade_.tradeId)`, referencing the `parentTrade` entity (same table)
- `Parent Trade -> Parent Trade Id` uses `FieldPath.of(Trade_.parentTrade).to(Trade_.parentTrade).to(Trade_.tradeId)`, referencing parent's parent trade
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/dot-notation-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/DotNotationService.java)

import ShowSqlMonitor from './show-sql-monitor';
import LazyGrid from './lazy-grid';
import DotNotationGrid from './dot-notation-grid';
import DotNotationFlatDataGrid from './dot-notation-flat-data-grid';

<ShowSqlMonitor serviceUrls={['/docs/dot-notation/getRows']}>
<LazyGrid>
<DotNotationGrid></DotNotationGrid>
</LazyGrid>
</ShowSqlMonitor>

### Suppress Field Dot Notation - Flat data

By default, dot notation fields are returned as **nested JSON objects** to align with AG Grid's default behavior (check response body in previous grid).

To return flat keys (e.g., `"category.name": "Value"`), you must enable `suppressFieldDotNotation` in the `QueryBuilder`.

```java
QueryBuilder<Entity, Long, Void> queryBuilder = QueryBuilder.builder(Entity.class, Entity_.id, entityManager)
    .colDefs(
        // ...col defs
    )
    .suppressFieldDotNotation(true)
    .build();
```

:::warning Client-Side Configuration
If you enable `suppressFieldDotNotation(true)` in the backend, you must also set [suppressFieldDotNotation](https://www.ag-grid.com/react-data-grid/grid-options/#reference-columns-suppressFieldDotNotation) to `true` in your AG Grid options on the frontend to ensure the grid treats dots as literal characters.
:::

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/dot-notation-flat-data-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/DotNotationService.java)

<ShowSqlMonitor serviceUrls={['/docs/dot-notation/flat-data/getRows']}>
<LazyGrid>
<DotNotationFlatDataGrid></DotNotationFlatDataGrid>
</LazyGrid>
</ShowSqlMonitor>

