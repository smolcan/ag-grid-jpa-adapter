---
sidebar_position: 2
---

# Column Advanced Filter Model
Advanced filter model for a single condition.

```typescript title="Column Advanced Filter Model structure"
export type ColumnAdvancedFilterModel = 
    TextAdvancedFilterModel | 
    NumberAdvancedFilterModel | 
    BooleanAdvancedFilterModel | 
    DateAdvancedFilterModel | 
    DateStringAdvancedFilterModel | 
    ObjectAdvancedFilterModel;
```

In AG Grid JPA Adapter is column advanced filter model implemented in class [ColumnAdvancedFilterModel](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/advanced/ColumnAdvancedFilterModel.java).