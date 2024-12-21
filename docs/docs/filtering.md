---
sidebar_position: 3
---

# Filtering
[Filtering](https://ag-grid.com/angular-data-grid/server-side-model-filtering/) in AG Grid JPA Adapter supports 
[Column filtering](https://ag-grid.com/angular-data-grid/filtering/) and 
[Advanced filtering](https://ag-grid.com/angular-data-grid/filter-advanced/).

In [ServerSideGetRowsRequest](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/request/ServerSideGetRowsRequest.java)
we receive filter model as:
```java title="Filter model in ServerSideGetRowsRequest"
private Map<String, Object> filterModel;
```
We further analyze this filter model and decide, what kind of filter we received ().

## Column Filtering
If received filter model is in this format:
```typescript title="Column Filter format"
export interface FilterModel {
    [colId: string]: any;
}
```
then we know we received column filter.
Every key of the object is the column id, and every value is the filter model.

For example:
```javascript title="Example of column filter model from AG Grid documentation"
{
    filterModel: {
        athlete: {
            filterType: 'text',
            type: 'contains',
            filter: 'fred'
        },
        year: {
            filterType: 'number',
            type: 'greaterThan',
            filter: 2005,
            filterTo: null
        }
    }
}
```

All of the column filters contains **filterType** field.
Default built-in filters in AG Grid are:
- [Text Filter](https://ag-grid.com/angular-data-grid/filter-text/)
- Number Filter
- Date Filter
- Set Filter
- Multi Filter

These filters are recognized and applied in this solution out of the box without any further configuration needed.

### Text Filter
[Text Filter](https://ag-grid.com/angular-data-grid/filter-text/) is used to filter string data.
```javascript title="Example of column definition with text filter"
{
    field: 'country',
    filter: 'agTextColumnFilter'
}
```

We receive Text Filter in this format:
```javascript title="Example of received text filter in filter model in request"
filterModel: {
    athlete: {
        filterType: 'text',
        type: 'contains',
        filter: 'fred'
    }
},
```

Text filter is recognized as it always has **filterType** property that has value **'text'**.

In AG Grid JPA Adapter is text filter implemented in class [TextFilter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/filter/simple/TextFilter.java).

Text filter supports these types:
- empty
- blank
- notBlank
- equals
- notEqual
- contains
- notContains
- startsWith
- endsWith

### Number Filter
### Date Filter
### Set Filter
### Multi Filter
### Register custom column filter