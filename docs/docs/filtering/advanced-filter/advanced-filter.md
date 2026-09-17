---
sidebar_position: 5
---

# Advanced Filter


## Enable Advanced filter
To enable the advanced filter, set the value of the `enableAdvancedFilter` variable in `QueryBuilder` to `true`:

```java
this.queryBuilder = QueryBuilder.builder(Entity.class, Entity_.id, entityManager)
                .colDefs(
                        // colDefs
                )
                .enableAdvancedFilter(true) // enable advanced filtering
                .build();
```

For a column to be filterable in the Advanced Filter, it must have a filter defined in its `ColDef`.

If a column does not have a filter set in `ColDef`, attempting to apply an Advanced Filter on it will result in an exception.

## Filter Params

**Filter parameters** are taken from `ColDef`.
- **Text & Object Filters** → [TextFilterParams](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/TextFilterParams.java)
- **Date & DateString Filters** → [DateFilterParams](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/DateFilterParams.java)
- **Number Filters** → [NumberFilterParams](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/NumberFilterParams.java)
- **Boolean Filters** → No filter parameters
- **Set Filters** → [SetFilterParams](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/SetFilterParams.java)

## Grid using Server Side Advanced Filter

- `Product` uses a set filter
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/filtering/advanced-filter/advanced-filter-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/AdvancedFilterService.java)


import ShowSqlMonitor from './../../show-sql-monitor';
import AdvancedFilterGrid from './advanced-filter-grid';
import LazyGrid from '../../lazy-grid';

<ShowSqlMonitor serviceUrls={['/docs/filtering/advanced-filter/getRows', '/docs/filtering/advanced-filter/supplySetFilterValues']}>
<LazyGrid>
<AdvancedFilterGrid></AdvancedFilterGrid>
</LazyGrid>
</ShowSqlMonitor>

## Custom Filter Options

Register each option under its `displayKey`, which AG Grid sends as the condition's `type`:

```java
this.queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
        .colDefs(
                ColDef.builder(Trade_.currentValue)
                        .filter(new AgNumberColumnFilter<>())
                        .build()
        )
        .enableAdvancedFilter(true)
        .registerCustomAdvancedFilter("betweenExclusive", BetweenExclusiveFilterModel::new)
        .build();
```

The function receives the raw condition and returns the filter model:

```java
public class BetweenExclusiveFilterModel extends ColumnAdvancedFilterModel<Trade, BigDecimal> {

    private final BigDecimal from;
    private final BigDecimal to;

    public BetweenExclusiveFilterModel(Map<String, Object> filter) {
        super("number", FieldPath.of(Trade_.currentValue));
        // { filterType: 'number', colId: 'currentValue', type: 'betweenExclusive', filter: 30, filterTo: 40 }
        this.from = new BigDecimal(filter.get("filter").toString());
        this.to = new BigDecimal(filter.get("filterTo").toString());
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<? extends Trade> root) {
        Expression<BigDecimal> currentValue = this.getColumnField().getExpression(cb, root);
        return cb.and(cb.gt(currentValue, this.from), cb.lt(currentValue, this.to));
    }
}
```

### Grid using Custom Filter Options

- `Portfolio` offers `Starts with vowel` (no input)
- `Current Value` offers `Between (Exclusive)` (two inputs)
- `Birth Date` offers `Same year as` (one input)
- The last filter model sent to the server is shown below the grid
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/filtering/advanced-filter/advanced-filter-custom-options-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/AdvancedFilterService.java)

import AdvancedFilterCustomOptionsGrid from './advanced-filter-custom-options-grid';

<ShowSqlMonitor serviceUrls={['/docs/filtering/advanced-filter/custom-filter-options/getRows']}>
<LazyGrid>
<AdvancedFilterCustomOptionsGrid></AdvancedFilterCustomOptionsGrid>
</LazyGrid>
</ShowSqlMonitor>
