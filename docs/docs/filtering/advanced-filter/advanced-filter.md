---
sidebar_position: 5
---

# Advanced Filter


## Enable Advanced filter
To enable the advanced filter, set the value of the `enableAdvancedFilter` variable in `QueryBuilder` to `true`:

```java
this.queryBuilder = QueryBuilder.builder(Entity.class, entityManager)
                .colDefs(
                        // colDefs
                )
                .enableAdvancedFilter(true) // enable advanced filtering
                .build();
```

For a column to be filterable in the Advanced Filter, it must have a filter defined in its `ColDef`.

If a column does not have a filter set in `ColDef`, attempting to apply an Advanced Filter on it will result in an exception.

## Filter Params

**Filter parameters** are taken from `ColDef`.
- **Text & Object Filters** → [TextFilterParams](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/TextFilterParams.java)
- **Date & DateString Filters** → [DateFilterParams](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/DateFilterParams.java)
- **Number Filters** → [NumberFilterParams](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/NumberFilterParams.java)
- **Boolean Filters** → No filter parameters
