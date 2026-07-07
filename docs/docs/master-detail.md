---
sidebar_position: 10
---

# Master Detail

The adapter supports AG Grid's [Master / Detail](https://www.ag-grid.com/react-data-grid/master-detail/) view, allowing you to nest grids inside rows.

import ShowSqlMonitor from './show-sql-monitor';
import MasterDetailGrid from './master-detail-grid';
import MasterDetailEagerGrid from './master-detail-eager-grid'
import MasterDetailDynamicRowsGrid from './master-detail-dynamic-rows-grid'
import MasterDetailTreeDataGrid from './master-detail-tree-data-grid'
import MasterDetailCustomDetailConditionGrid from './master-detail-custom-detail-condition-grid'
import LazyGrid from './lazy-grid';

## Enabling Master Detail

To enable Master/Detail, set `.masterDetail(true)` in the builder.

Unlike standard configuration, Master/Detail settings are grouped into a `MasterDetailParams` object. You must configure **what** to display (the detail entity class and columns) and **how** to link the detail records to the master record.

The type parameters are `QueryBuilder<E, E_ID, D>` (master entity, master ID type, detail entity). Pass the master's `@Id` attribute as the second argument and the detail entity class as the third: `QueryBuilder.builder(masterClass, masterPrimaryField, detailClass, em)`.

### Configuration Parameters

| Method | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `masterDetail` | `boolean` | **Yes** | Enables the functionality. |
| `builder(…, primaryField, …)` | `SingularAttribute<E, E_ID>` | **Yes** | The **Master** entity's `@Id` attribute (e.g. `MasterEntity_.id`), passed as the second argument to `QueryBuilder.builder(...)`. |
| `masterDetailParams` | `MasterDetailParams` | **Yes*** | Configuration object containing detail class, columns, and joining logic. |

*\*Required unless using dynamic params.*

### Relationship Mapping

Inside `MasterDetailParams`, you must define how the adapter finds the detail records for a specific master row. Provide **one** of the following:

1.  **Reference Field (`detailMasterReferenceField`):** Use if the Detail entity has a `@ManyToOne` mapping to the Master. Pass the detail's metamodel attribute, e.g. `DetailEntity_.parentEntity`.
2.  **ID Field (`detailMasterIdField`):** Use if the Detail entity only holds the raw Foreign Key ID. Pass the metamodel attribute, e.g. `DetailEntity_.parentId`.
3.  **Custom Predicate (`createMasterRowPredicate`):** Use for complex joining logic.


```java
QueryBuilder.builder(MasterEntity.class, MasterEntity_.id, DetailEntity.class, em)
    .masterDetail(true)
    
    // Group Detail Configuration
    .masterDetailParams(
        QueryBuilder.MasterDetailParams.<MasterEntity, Long, DetailEntity>builder()
            .detailClass(DetailEntity.class)
            .detailColDefs(
                ColDef.builder(DetailEntity_.detailId).build(),
                ColDef.builder(DetailEntity_.amount).build()
            )
            // Define Relationship (Choose One)
            .detailMasterReferenceField(DetailEntity_.parentEntity) 
            // .detailMasterIdField(DetailEntity_.parentId)
            .build()
    )
    .build();
```

## Lazy vs Eager loading Detail Rows

The adapter supports two strategies for loading detail data: Lazy (default) and Eager.

### Lazy Loading (Default)

Lazy loading is the default mode — you do not need to call `.masterDetailLazy(true)` explicitly unless overriding a previous setting.

In this mode, detail data is not sent with the master rows.
When a user expands a row in AG Grid, the grid sends a separate server-side request.
You should expose a separate endpoint that calls `getDetailRowData(masterRow)`.


- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/master-detail-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/MasterDetailService.java)

<ShowSqlMonitor serviceUrls={['/docs/master-detail/getRows', '/docs/master-detail/getDetailRowData']}>
<LazyGrid>
    <MasterDetailGrid></MasterDetailGrid>
</LazyGrid>
</ShowSqlMonitor>

### Eager Loading

`masterDetailLazy(false)`

In this mode, the adapter fetches detail rows immediately for every master row returned and embeds them directly into the response JSON. 
This reduces HTTP requests but increases payload size and backend load.

:::info Batch Fetching Optimization
By default, the adapter optimizes eager loading using a **Batch Fetching** strategy. It executes exactly **2 database queries** per request, regardless of page size:
1.  One query to fetch the Master rows.
2.  One query to fetch all Detail rows for those masters (using an `IN` clause).
    :::

:::warning Performance Fallback (N+1)
The optimization above is disabled if you use **`dynamicMasterDetailParams`** or a custom **`createMasterRowPredicate`**.

In these dynamic scenarios, the adapter must fall back to the **N+1 strategy**, executing a separate database query for **each** master row returned. Use dynamic configuration with caution on large page sizes.
:::

Requirements:
1. You must provide `.masterDetailRowDataFieldName(String)` on the main builder.
2. This field name must not exist in the `detailColDefs`.

```java
QueryBuilder.builder(MasterEntity.class, MasterEntity_.id, DetailEntity.class, em)
    .masterDetail(true)
    .masterDetailLazy(false) // Turn off lazy loading
    .masterDetailRowDataFieldName("detailRows") // JSON key for nested list
    
    .masterDetailParams(
        QueryBuilder.MasterDetailParams.<MasterEntity, Long, DetailEntity>builder()
            .detailClass(DetailEntity.class)
            .detailColDefs(
                 ColDef.builder(DetailEntity_.detailId).build()
            )
            .detailMasterReferenceField(DetailEntity_.parentEntity)
            .build()
    )
    .build();
```

JSON Response example:
```json
[
  {
    "id": 1,
    "name": "Master Row A",
    "detailRows": [  // populated automatically
       { "detailId": 100, "amount": 50 },
       { "detailId": 101, "amount": 25 }
    ]
  }
]
```

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/master-detail-eager-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/MasterDetailService.java)

<ShowSqlMonitor serviceUrls={['/docs/master-detail/eager/getRows']}>
<LazyGrid>
    <MasterDetailEagerGrid></MasterDetailEagerGrid>
</LazyGrid>
</ShowSqlMonitor>

## Custom Detail Condition

If standard ID or Reference mapping is insufficient (e.g., for composite keys or complex conditional joining), you can define exact linking logic using `createMasterRowPredicate`.

This function provides access to the JPA `CriteriaBuilder`, the detail entity `Root`, and the raw `masterRow` data map, allowing you to construct any valid JPA Predicate to filter the detail records.

```java
QueryBuilder.builder(Trade.class, Trade_.tradeId, Trade.class, entityManager)
                .colDefs(...)
                .masterDetail(true)
                .masterDetailParams(
                        QueryBuilder.MasterDetailParams.<Trade, Long, Trade>builder()
                                .detailClass(Trade.class)
                                .detailColDefs(...)
                                .createMasterRowPredicate((cb, detailRoot, masterRow) -> {
                                    // detail will have all the trades that have the same submitter
                                    var submitterObj = (Map<String, Object>) masterRow.get("submitter");
                                    if (submitterObj == null || submitterObj.isEmpty()) {
                                        return cb.or();
                                    }

                                    Long submitterId = Optional.ofNullable(submitterObj.get("id")).map(String::valueOf).map(Long::parseLong).orElse(null);
                                    Path<?> path = detailRoot.get("submitter").get("id");
                                    if (submitterId == null) {
                                        return cb.isNull(path);
                                    } else {
                                        return cb.equal(path, submitterId);
                                    }
                                })
                                .build()
                )

                .build();
```

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/master-detail-custom-detail-condition-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/MasterDetailService.java)

<ShowSqlMonitor serviceUrls={['/docs/master-detail/custom-detail-condition/getRows', '/docs/master-detail/custom-detail-condition/getDetailRows']}>
<LazyGrid>
    <MasterDetailCustomDetailConditionGrid></MasterDetailCustomDetailConditionGrid>
</LazyGrid>
</ShowSqlMonitor>

## Dynamic Detail Definitions

For advanced use cases, the configuration of the detail grid can change based on the data in the master row. 
For example, a "Vehicle" master row might show a different set of detail columns depending on the vehicle type.

You can provide a function to resolve the `MasterDetailParams` dynamically at runtime using `dynamicMasterDetailParams`.

:::info Runtime resolution
This function receives the masterRow (as a Map) as an argument, allowing you to inspect values to decide which columns and relationship to use.

The detail **entity class** is fixed for a given `QueryBuilder` (the second argument to `builder(...)`); dynamic params vary the columns, relationship and predicate — not the entity type.
:::

```java
QueryBuilder.builder(Vehicle.class, Vehicle_.id, VehicleDetail.class, em)
    .masterDetail(true)
    
    // Dynamic Configuration
    .dynamicMasterDetailParams(masterRow -> {
        String type = (String) masterRow.get("type");
        
        if ("CAR".equals(type)) {
            return QueryBuilder.MasterDetailParams.<Vehicle, Long, VehicleDetail>builder()
                    .detailClass(VehicleDetail.class)
                    .detailColDefs(
                        ColDef.builder(VehicleDetail_.wheels).build(),
                        ColDef.builder(VehicleDetail_.engine).build()
                    )
                    .detailMasterReferenceField(VehicleDetail_.vehicle)
                    .build();
        } else {
             return QueryBuilder.MasterDetailParams.<Vehicle, Long, VehicleDetail>builder()
                    .detailClass(VehicleDetail.class)
                    .detailColDefs(
                        ColDef.builder(VehicleDetail_.propeller).build(),
                        ColDef.builder(VehicleDetail_.sails).build()
                    )
                    .detailMasterReferenceField(VehicleDetail_.vehicle)
                    .build();
        }
    })
    .build();
```

:::warning Validation 
If you enable masterDetail, you must provide either the static definition (`masterDetailParams`) OR the dynamic definition (`dynamicMasterDetailParams`). 
:::

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/master-detail-dynamic-rows-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/MasterDetailService.java)


<ShowSqlMonitor serviceUrls={['/docs/master-detail/dynamic/getRows', '/docs/master-detail/dynamic/getDetailRowData']}>
<LazyGrid>
    <MasterDetailDynamicRowsGrid></MasterDetailDynamicRowsGrid>
</LazyGrid>
</ShowSqlMonitor>


## Combining Tree Data with Master Detail

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/master-detail-tree-data-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/MasterDetailService.java)


<ShowSqlMonitor serviceUrls={['/docs/master-detail/tree/getRows', '/docs/master-detail/tree/getDetailRowData']}>
<LazyGrid>
    <MasterDetailTreeDataGrid></MasterDetailTreeDataGrid>
</LazyGrid>
</ShowSqlMonitor>
