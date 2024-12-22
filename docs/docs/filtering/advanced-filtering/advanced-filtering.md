---
sidebar_position: 2
---

# Advanced Filtering
[Advanced Filtering](https://ag-grid.com/angular-data-grid/filter-advanced/) is applied when **enableAdvancedFilter** property is set to **true**.

Advanced filter model is defined like this:
```typescript title="Advanced Filter Model structure"
export type AdvancedFilterModel = JoinAdvancedFilterModel | ColumnAdvancedFilterModel;
/** Represents a series of filter conditions joined together. */
export interface JoinAdvancedFilterModel {
    filterType: 'join';
    /** How the conditions are joined together */
    type: 'AND' | 'OR';
    /** The filter conditions that are joined by the `type` */
    conditions: AdvancedFilterModel[];
}
export type ColumnAdvancedFilterModel = 
    TextAdvancedFilterModel | 
    NumberAdvancedFilterModel | 
    BooleanAdvancedFilterModel | 
    DateAdvancedFilterModel | 
    DateStringAdvancedFilterModel | 
    ObjectAdvancedFilterModel;
```
As you can see from the structure, Advanced Filter model can be either [JoinAdvancedFilterModel](./join-advanced-filter-model/join-advanced-filter-model.md) or [ColumnAdvancedFilterModel](./column-advanced-filter-model/column-advanced-filter-model.md);

In AG Grid JPA Adapter is advanced filter model implemented in class [AdvancedFilterModel](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/github/smolcan/aggrid/jpa/adapter/filter/advanced/AdvancedFilterModel.java).
