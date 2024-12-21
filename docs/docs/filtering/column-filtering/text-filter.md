---
sidebar_position: 1
---

# Text Filter
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