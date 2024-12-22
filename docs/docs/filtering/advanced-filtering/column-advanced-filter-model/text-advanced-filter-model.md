---
sidebar_position: 1
---

# Text Advanced Filter Model
Advanced filter model for advanced filtering of string data.

We receive Text Advanced Filter Model in this format:
```javascript title="Example of received text advanced filter in filter model in request"
filterModel: {
    filterType: 'text',
    colId: 'sport',
    type: 'endsWith',
    filter: 'ing'
}
```


In AG Grid JPA Adapter is text advanced filter model implemented in class [TextAdvancedFilterModel](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/advanced/column/TextAdvancedFilterModelType.java).


Text advanced filter model supports these types:
- blank
- notBlank
- equals
- notEqual
- contains
- notContains
- startsWith
- endsWith