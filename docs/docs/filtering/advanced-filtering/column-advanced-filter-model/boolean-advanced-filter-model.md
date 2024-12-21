---
sidebar_position: 3
---

# Boolean Advanced Filter Model
Advanced filter model for advanced filtering of boolean data.

We receive Boolean Advanced Filter Model in this format:
```javascript title="Example of received boolean advanced filter in filter model in request"
filterModel: {
    filterType: 'boolean',
    colId: 'isWinner',
    type: 'true'
}
```

In AG Grid JPA Adapter is boolean advanced filter model implemented in class [BooleanAdvancedFilterModel](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/filter/advanced/column/BooleanAdvancedFilterModel.java).


Boolean advanced filter model supports these types:
- blank
- notBlank
- true
- false
