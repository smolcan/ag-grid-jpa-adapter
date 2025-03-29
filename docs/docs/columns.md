---
sidebar_position: 2
---

# Columns

In order to define which columns should be returned to the client, we need to use [ColDefs](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/column/ColDef.java) objects. 
Each column that we want to include in the AG Grid response must be explicitly defined in the [ColDefs](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/column/ColDef.java).

## Defining Columns

Each column is represented by a [ColDefs](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/column/ColDef.java) object, 
which specifies various properties of the column. Below are the available fields for defining a column:

---

### `field` (required)
:::info Type
`String`
:::
The name of the entity field that this column represents.

---

### `sortable`
:::info Type
`Boolean`
:::
Determines whether the column can be sorted.

**Default:** `true`

---

### `filter`
:::info Type
[IFilter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/IFilter.java)
:::
Specifies the filter type for the column.

**Default:** [AgTextColumnFilter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/simple/AgTextColumnFilter.java)

**Options:**
- A specific filter type (e.g., [AgNumberColumnFilter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/simple/AgNumberColumnFilter.java), [AgSetColumnFilter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/AgSetColumnFilter.java)...)
- `false` (if the column should not be filterable)
- Any implementation of the [IFilter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/IFilter.java) interface

---

### `allowedAggFuncs`
:::info Type
Set\<[AggregationFunction](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/request/AggregationFunction.java)\>
:::
Defines which aggregation functions are allowed for this column.

**Default:** All available aggregation functions

---

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
