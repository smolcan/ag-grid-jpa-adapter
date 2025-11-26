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
| **`field`** *(required)* | `string`                                                                                                                                     | —                                                                                                                                                                                  | The name of the entity field.                                                                                                                                                                                                                                                                                                                  |
| **`sortable`**           | `boolean`                                                                                                                                    | `true`                                                                                                                                                                             | Enables or disables sorting.                                                                                                                                                                                                                                                                                                                   |
| **`filter`**             | [`IFilter`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/IFilter.java) | [`AgTextColumnFilter`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/simple/AgTextColumnFilter.java) | Defines the filter type. <br/> Supports: <br/> ✅ Custom `IFilter` implementations <br/> ✅ Built-in filters (e.g., [`AgNumberColumnFilter`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/simple/AgNumberColumnFilter.java)) <br/> ❌ `false` (disables filtering) |
| **`enableValue`**        | `boolean`                                                                                                                                    | `false`                                                                                                                                                                            | Set to `true` if you want to be able to row group by this column.                                                                                                                                                                                                                                                                              |
| **`enableRowGroup`**     | `boolean`                                                                                                                                    | `false`                                                                                                                                                                            | Set to `true` if you want to be able to aggregate by this column.                                                                                                                                                                                                                                                                              |
| **`enablePivot`**     | `boolean`                                                                                                                                    | `false`                                                                                                                                                                            | Set to `true` if you want to be able to pivot by this column.                                                                                                                                                                                                                                                                                  |
| **`allowedAggFuncs`**    | `Set<AggregationFunction>`                                                                                                                   | All available                                                                                                                                                                      | Defines allowed aggregation functions.                                                                                                                                                                                                                                                                                                         |


## Example Usage

```java
ColDef priceColumn = ColDef.builder()
    .field("price")
    .sortable(true)
    .filter(new AgSetColumnFilter())
    .allowedAggFuncs(AggregationFunction.avg, AggregationFunction.count)
    .build();

ColDef nameColumn = ColDef.builder()
    .field("name")
    .sortable(false)
    .filter(false)
    .build();

QueryBuilder<Entity> queryBuilder = QueryBuilder.builder(Entity.class, entityManager)
    .colDefs(priceColumn, nameColumn)
    .build();
```

## Dot notation

The adapter fully supports querying nested fields using standard dot notation (e.g., `"category.name"` or `"user.address.city"`).

:::info JPA Requirement
For dot notation to work, the referenced path must correspond to a mapped relationship (e.g., `@ManyToOne`, `@OneToOne`) within your JPA Entity.
:::

### Suppress Field Dot Notation - Flat data

By default, dot notation fields are returned as **nested JSON objects** to align with AG Grid's default behavior.

To return flat keys (e.g., `"category.name": "Value"`), you must enable suppressFieldDotNotation in the `QueryBuilder`.

```java
QueryBuilder<Entity> queryBuilder = QueryBuilder.builder(Entity.class, entityManager)
    .colDefs(
        // ...col defs
    )
    .suppressFieldDotNotation(true)
    .build();
```

:::warning Client-Side Configuration
If you enable `suppressFieldDotNotation(true)` in the backend, you must also set [suppressFieldDotNotation](https://www.ag-grid.com/react-data-grid/grid-options/#reference-columns-suppressFieldDotNotation) to `true` in your AG Grid options on the frontend to ensure the grid treats dots as literal characters.
:::

