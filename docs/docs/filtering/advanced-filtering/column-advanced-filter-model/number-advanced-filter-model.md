---
sidebar_position: 2
---

# Number Advanced Filter Model
Advanced filter model for advanced filtering of numeric data.

We receive Number Advanced Filter Model in this format:
```javascript title="Example of received number advanced filter in filter model in request"
filterModel: {
    filterType: 'number',
    colId: 'score',
    type: 'lessThan',
    filter: 80
}
```

In AG Grid JPA Adapter is number advanced filter model implemented in class [NumberAdvancedFilterModel](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/filter/advanced/column/NumberAdvancedFilterModel.java).


Number advanced filter model supports these types:
- blank
- notBlank
- equals
- notEqual
- lessThen
- lessThanOrEqual
- greaterThan
- greaterThanOrEqual
