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
| **`field`** *(required)* | `SingularAttribute` / `FieldPath` / `ComputedField`                                                                                                                                     | —                                                                                                                                                                                  | Where the column reads its value, passed to `ColDef.builder(...)`: a JPA metamodel attribute (e.g. `Trade_.price`), a `FieldPath` for nested paths, or a `ComputedField` for a database expression.                                                                                                                                                                                                                                                                                                                  |
| **`sortable`**           | `boolean`                                                                                                                                    | `true`                                                                                                                                                                             | Enables or disables sorting.                                                                                                                                                                                                                                                                                                                   |
| **`filter`**             | [`IFilter`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/IFilter.java) | — | Defines the filter type (no default — a column has no filter unless one is set). <br/> Supports: <br/> ✅ Custom `IFilter` implementations <br/> ✅ Built-in filters (e.g., [`AgNumberColumnFilter`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/simple/AgNumberColumnFilter.java)) <br/> ⚠️ Omit `.filter(...)` to leave the column without a filter |
| **`enableValue`**        | `boolean`                                                                                                                                    | `false`                                                                                                                                                                            | Set to `true` if you want to be able to aggregate by this column.                                                                                                                                                                                                                                                                              |
| **`enableRowGroup`**     | `(boolean, Function<String, T>)`                                                                                                                                    | `false`                                                                                                                                                                            | Enable row grouping on this column. Because AG Grid sends group keys as strings, you must also pass a converter from the string group key to the column type `T`, e.g. `.enableRowGroup(true, BigDecimal::new)`.                                                                                                                                                                                                                                                                              |
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

## Computed columns

A column does not have to map to an entity attribute. [`ComputedField`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/column/ComputedField.java) builds the column from a JPA `Expression`, so the value is produced by the database.

| Property                            | Type                                            | Description                                                            |
|-------------------------------------|-------------------------------------------------|------------------------------------------------------------------------|
| **`name`**                          | `String`                                        | The column name AG Grid sends and the field name in the response.       |
| **`javaType`**                      | `Class<T>`                                      | The type the expression evaluates to.                                   |
| **`expressionFunction`**            | `BiFunction<CriteriaBuilder, Root<E>, Expression<T>>` | Builds the expression for a given query root.                     |

The expression is resolved wherever the column is used, so a computed column behaves like a mapped one:
selecting, filtering, sorting, row grouping, aggregation, pivoting, quick filter, advanced filter and
`supplySetFilterValues` all work on it.

```java
ComputedField<Trade, String> valueBand = ComputedField.<Trade, String>builder()
    .name("valueBand")
    .javaType(String.class)
    .expressionFunction((cb, root) -> cb.<String>selectCase()
        .when(cb.greaterThan(root.get(Trade_.currentValue), BigDecimal.ZERO), "POSITIVE")
        .otherwise("NON_POSITIVE"))
    .build();

QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
    .colDefs(
        ColDef.builder(Trade_.tradeId).build(),
        ColDef.builder(valueBand)
            .filter(AgSetColumnFilter.forString())
            .enableRowGroup(true, key -> key)
            .build()
    )
    .build();
```

`Value Change` is `currentValue - previousValue`, `Value Band` is a `CASE` over those same two columns and
`Portfolio / Book` concatenates two text columns. None of the three exists on the entity.

- try filtering or sorting on a computed column, or drag `Value Band` into the row group panel
- the `Value Band` set filter values come from `supplySetFilterValues`, read straight from the expression
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/computed-columns-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/ComputedColumnsService.java)

import ComputedColumnsGrid from './computed-columns-grid';

<ShowSqlMonitor serviceUrls={['/docs/computed-columns/getRows', '/docs/computed-columns/supplySetFilterValues']}>
<LazyGrid>
<ComputedColumnsGrid></ComputedColumnsGrid>
</LazyGrid>
</ShowSqlMonitor>

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

