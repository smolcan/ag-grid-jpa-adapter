---
sidebar_position: 4
---

# Date Advanced Filter Model
Advanced filter model for advanced filtering of date data.
We receive Advanced Date Filter in this format:
```javascript title="Example of received advanced date filter in filter model in request"
filterModel: {
    filterType: 'date',
    type: 'greaterThanOrEqual',
    filter: '2001-10-18'
},
```

In AG Grid JPA Adapter is number advanced filter model implemented in class [DateAdvancedFilterModel](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/advanced/column/DateAdvancedFilterModel.java).


Date advanced filter model supports these types:
- blank
- notBlank
- equals
- notEqual
- lessThen
- lessThanOrEqual
- greaterThan
- greaterThanOrEqual
