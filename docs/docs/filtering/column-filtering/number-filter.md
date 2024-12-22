---
sidebar_position: 2
---

# Number Filter
[Number Filter](https://ag-grid.com/angular-data-grid/filter-number/) is used to filter numeric data.
```javascript title="Example of column definition with number filter"
{
    field: 'age',
    filter: 'agNumberColumnFilter'
}
```

We receive Number Filter in this format:
```javascript title="Example of received number filter in filter model in request"
filterModel: {
    height: {
        filterType: 'number',
        type: 'inRange',
        filter: 12.35,
        filterTo: 14.28
    }
},
```

Number filter is recognized as it always has **filterType** property that has value **'number'**.

In AG Grid JPA Adapter is number filter implemented in class [NumberFilter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/github/smolcan/aggrid/jpa/adapter/filter/simple/NumberFilter.java).

Number filter supports these types:
- empty
- blank
- notBlank
- equals
- notEqual
- lessThen
- lessThanOrEqual
- greaterThan
- greaterThanOrEqual
- inRange