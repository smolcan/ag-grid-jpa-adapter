---
sidebar_position: 5
---

# Set Filter
[Set Filter](https://ag-grid.com/angular-data-grid/filter-set/) is used to filter on sets of data.
```javascript title="Example of column definition with set filter"
{
    field: 'portfolio',
    filter: 'agSetColumnFilter',
    filterParams: {
        values: ['Portfolio1', 'Portfolio2', 'Portfolio3']
    }
}
```

We receive Set Filter in this format:
```javascript title="Example of received set filter in filter model in request"
filterModel: {
    portfolio: {
        filterType: 'set',
        values: ['Portfolio1', 'Portfolio2']
    }
},
```

Date filter is recognized as it always has **filterType** property that has value **'set'**.

In AG Grid JPA Adapter is set filter implemented in class [SetFilter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/github/smolcan/aggrid/jpa/adapter/filter/simple/SetFilter.java).
