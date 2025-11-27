---
sidebar_position: 10
---

# Master Detail

The adapter supports AG Grid's [Master / Detail](https://www.ag-grid.com/react-data-grid/master-detail/) view, allowing you to nest grids inside rows.

## Enabling Master Detail

To enable Master/Detail, set `.masterDetail(true)` in the builder. 
You must configure **what** to display (the detail entity class and columns) and **how** to link the detail records to the master record.

### Configuration Parameters

| Method | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `masterDetail` | `boolean` | **Yes** | Enables the functionality. |
| `detailClass` | `Class<?>` | **Yes** | The entity class for the detail grid. |
| `detailColDefs` | `ColDef...` | **Yes** | Column definitions for the detail grid. |
| `primaryFieldName` | `String` | **Yes** | The ID field name of the **Master** entity. |

### Relationship Mapping

You must define how the adapter finds the detail records for a specific master row. Provide **one** of the following:

1.  **Reference Field (`detailMasterReferenceField`):** Use if the Detail entity has a `@ManyToOne` mapping to the Master.
2.  **ID Field (`detailMasterIdField`):** Use if the Detail entity only holds the raw Foreign Key ID.
3.  **Custom Predicate (`createMasterRowPredicate`):** Use for complex joining logic.

```java
QueryBuilder.builder(MasterEntity.class, em)
    .masterDetail(true)
    .primaryFieldName("id") // Master ID
    
    // Define Detail Structure
    .detailClass(DetailEntity.class)
    .detailColDefs(
        ColDef.builder().field("detailId").build(),
        ColDef.builder().field("amount").build()
    )
    
    // Define Relationship (Choose One)
    .detailMasterReferenceField("parentEntity") 
    // .detailMasterIdField("parentId")
    
    .build();
```

## Lazy vs Eager loading Detail Rows

The adapter supports two strategies for loading detail data: Lazy (default) and Eager.

### Lazy Loading (Default)

`masterDetailLazy(true)`

In this mode, detail data is not sent with the master rows. 
When a user expands a row in AG Grid, the grid sends a separate server-side request. 
You should expose a separate endpoint that calls `getDetailRowData(masterRow)`.

### Eager Loading

`masterDetailLazy(false)`

In this mode, the adapter fetches detail rows immediately for every master row returned and embeds them directly into the response JSON. 
This reduces HTTP requests but increases payload size and backend load.

:::warning Performance Impact
Enabling eager loading triggers the **N+1 problem**. The adapter executes a separate database query to fetch details for **each** master row returned.

For example, if your grid page size is 100, the adapter will execute **101 database queries** (1 for master rows + 100 for details). Use this mode with caution on large datasets.
:::

Requirements:
1. You must provide `.masterDetailRowDataFieldName(String)`.
2. This field name must not exist in the `detailColDefs`.

```java
QueryBuilder.builder(MasterEntity.class, em)
    .masterDetail(true)
    .masterDetailLazy(false) // Turn off lazy loading
    .masterDetailRowDataFieldName("detailRows") // JSON key for nested list
    // ... other config
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

## Dynamic Detail Definitions

For advanced use cases, the structure of the detail grid can change based on the data in the master row. For example, 
a "Vehicle" master row might show "Car Details" or "Boat Details" depending on the vehicle type.

You can provide functions to resolve the class and columns dynamically at runtime using `dynamicDetailClass` and `dynamicColDefs`.

:::info 
Runtime Resolution These functions receive the masterRow (as a Map) as an argument, allowing you to inspect values to make decisions. 
:::

```java
QueryBuilder.builder(Vehicle.class, em)
    .masterDetail(true)
    .primaryFieldName("id")
    
    // Dynamic Class Resolution
    .dynamicDetailClass(masterRow -> {
        String type = (String) masterRow.get("type");
        if ("CAR".equals(type)) return CarDetail.class;
        return BoatDetail.class;
    })
    
    // Dynamic Column Definitions
    .dynamicColDefs(masterRow -> {
        String type = (String) masterRow.get("type");
        if ("CAR".equals(type)) {
            return List.of(
                ColDef.builder().field("wheels").build(),
                ColDef.builder().field("engine").build()
            );
        } else {
             return List.of(
                ColDef.builder().field("propeller").build(),
                ColDef.builder().field("sails").build()
            );
        }
    })
    .build();
```

:::warning Validation 
If you enable masterDetail, you must provide either the static definition (detailClass/detailColDefs) OR the dynamic definition (dynamicDetailClass/dynamicColDefs). 
:::

[//]: # (import GridLoadingMessage from './grid-loading-message';)

[//]: # (import MasterDetailGrid from './master-detail-grid';)

[//]: # ()
[//]: # (<GridLoadingMessage>)

[//]: # (    <MasterDetailGrid></MasterDetailGrid>)

[//]: # (</GridLoadingMessage>)