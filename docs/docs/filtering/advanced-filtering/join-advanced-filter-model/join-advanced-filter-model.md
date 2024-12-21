---
sidebar_position: 1
---

# Join Advanced Filter Model
Advanced filter model for multiple conditions.

```typescript title="Join Advanced Filter Model structure"
export interface JoinAdvancedFilterModel {
    filterType: 'join';
    /** How the conditions are joined together */
    type: 'AND' | 'OR';
    /** The filter conditions that are joined by the `type` */
    conditions: AdvancedFilterModel[];
}
```

Example of received joined advanced filter model:
```javascript title="Join Advanced Filter Model example"
{
    filterModel: {
        filterType: 'join',
        type: 'AND',
        conditions: [
            {
                filterType: 'join',
                type: 'OR',
                conditions: [
                    {
                        filterType: 'number',
                        colId: 'age',
                        type: 'greaterThan',
                        filter: 23,
                    },
                    {
                        filterType: 'text',
                        colId: 'sport',
                        type: 'endsWith',
                        filter: 'ing',
                    }
                ]
            },
            {
                filterType: 'text',
                colId: 'country',
                type: 'contains',
                filter: 'united',
            }
        ]
    }
}
```

In AG Grid JPA Adapter is advanced filter model implemented in class [JoinAdvancedFilterModel](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/filter/advanced/JoinAdvancedFilterModel.java).
