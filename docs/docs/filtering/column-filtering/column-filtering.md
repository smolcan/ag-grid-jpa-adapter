---
sidebar_position: 1
---

# Column Filtering
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
- [Number Filter](https://ag-grid.com/angular-data-grid/filter-number/)
- [Date Filter](https://ag-grid.com/angular-data-grid/filter-date/)
- Combined Filter
- [Set Filter](https://ag-grid.com/angular-data-grid/filter-set/)
- [Multi Filter](https://ag-grid.com/angular-data-grid/filter-multi/)

These filters are recognized and applied in this solution out of the box without any further configuration needed.

In AG Grid JPA Adapter is column filter implemented in class [ColumnFilter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/simple/ColumnFilter.java).
