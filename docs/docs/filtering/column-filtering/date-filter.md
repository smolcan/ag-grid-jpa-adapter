---
sidebar_position: 3
---

# Date Filter
[Date Filter](https://ag-grid.com/angular-data-grid/filter-date/) is used to filter date data.
```javascript title="Example of column definition with date filter"
{
    field: 'birthDate',
    filter: 'agDateColumnFilter'
}
```

We receive Date Filter in this format:
```javascript title="Example of received date filter in filter model in request"
filterModel: {
    birthDate: {
        filterType: 'date',
        type: 'inRange',
        dateFrom: '2001-10-18 00:00:00',
        dateTo: '2003-10-18 00:00:00'
    }
},
```

Date filter is recognized as it always has **filterType** property that has value **'date'**.

In AG Grid JPA Adapter is date filter implemented in class [DateFilter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/simple/DateFilter.java).

Date filter supports these types:
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

