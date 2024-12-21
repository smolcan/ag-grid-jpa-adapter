---
sidebar_position: 4
---

# Combined Filter
If more than one Filter Condition is set, then multiple instances of the model are created and wrapped inside a Combined Model.
```typescript title="Combined Filter model interface definition"
export interface ICombinedSimpleModel<M extends ISimpleFilterModel> extends ProvidedFilterModel {
    operator: JoinOperator;
    conditions: M[];
}
export type JoinOperator = 'AND' | 'OR';
```

As you can see from the interface definition, it can wrap multiple instances of filters extending **ISimpleFilterModel** and join them by **JoinOperator**.
**ISimpleFilterModel** is extended only by [TextFilter](./text-filter.md), [NumberFilter](./number-filter.md) and [DateFilter](./date-filter.md) models.

```javascript title="Combined Filter model example for Text, Number and Date filter"
{
    filterModel: {
        // Combined Text Filter
        sport: {
            filterType: 'text',
            operator: 'OR',
            conditions: [
                {
                    filterType: 'text',
                    type: 'equals',
                    filter: 'Swimming'
                },
                {
                    filterType: 'text',
                    type: 'equals',
                    filter: 'Gymnastics'
                }
            ]
        },
        // Combined Number Filter
        timeInSeconds: {
            filterType: 'number',
            operator: 'OR',
            conditions: [
                {
                    filterType: 'number',
                    type: 'equals',
                    filter: 18
                },
                {
                    filterType: 'number',
                    type: 'equals',
                    filter: 20
                }
            ]
        },
        // Combined Date Filter
        eventDate: {
            filterType: 'date',
            operator: 'OR',
            conditions: [
                {
                    filterType: 'date',
                    type: 'equals',
                    dateFrom: '2004-08-29'
                },
                {
                    filterType: 'date',
                    type: 'equals',
                    dateFrom: '2008-08-24'
                }
            ]
        }
    }
}
```

Combined filter is recognized as it always has **filterType** property that has value  **'text'**, **'number'** or **'date'** and
also has **operator** field and **conditions** field.

In AG Grid JPA Adapter is date filter implemented in class [CombinedSimpleModel](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/com/aggrid/jpa/adapter/filter/simple/CombinedSimpleModel.java).