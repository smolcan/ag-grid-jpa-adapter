---
sidebar_position: 6
---

# Multi Filter
The [Multi Filter](https://ag-grid.com/angular-data-grid/filter-multi/) allows multiple Provided Filters or Custom Filters to be used on the same column.
```typescript title="Example of column definition with multi filter"
{
    field: 'portfolio',
    filter: 'agMultiColumnFilter',
    filterParams: {
        filters: [
          {
            filter: 'agSetColumnFilter',
            filterParams: {
                values: ['Portfolio1', 'Portfolio2', 'Portfolio3']
            }
          },
          {
            filter: 'agTextColumnFilter',
            filterParams: {
                defaultOption: "startsWith",
            } as ITextFilterParams,
          },
        ]
    }
},
```


We receive Multi Filter in this format:
```javascript title="Example of received multi filter in filter model in request"
filterModel: {
    portfolio: {
        filterType: 'multi',
        filterModels: [
            {
                filterType: 'set',
                values: ['Portfolio1', 'Portfolio2']
            },
            {
                filterType: 'text',
                type: 'contains',
                filter: 'port'
            }
        ]
    }
},
```

Text filter is recognized as it always has **filterType** property that has value **'multi'**.

In AG Grid JPA Adapter is multi filter implemented in class [MultiFilter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/simple/MultiFilter.java).

