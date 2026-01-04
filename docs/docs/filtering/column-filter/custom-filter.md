---
sidebar_position: 6
---

# Custom Filter

To implement a custom filter, follow these steps:


1. **Implement the `IFilterModel` interface**  
   Define your custom filter model by creating a class that implements the [`IFilterModel`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/IFilterModel.java) interface.

2. **Implement the `IFilterParams` interface**  
   Create a class representing the parameters for your filter by implementing the [`IFilterParams`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/IFilterParams.java) interface.

3. **Extend the `IFilter` abstract class**  
   Create a class representing your custom filter by extending the [`IFilter`](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/IFilter.java) abstract class.


## Example
We will create a custom number filter, that will filter the even/odd numbers.

### 1. **Implement the `IFilterModel` interface**  
Our filter model will have only one field, which is value (Even, Odd or All).
```java
public class CustomNumberFilterModel implements IFilterModel {
    private String value;
    // ...getters and setters
}
```

### 2.  **Implement the `IFilterParams` interface**
Additional parameters for our filter, for example, if we should include null values in results.
```java
public class CustomNumberFilterParams implements IFilterParams {
    private boolean includeNullValues;
    // ...getters and setters
}
```

### 3. **Extend the `IFilter` abstract class** 
Extend and overwrite the required methods.
```java
public class CustomNumberFilter extends IFilter<CustomNumberFilterModel, CustomNumberFilterParams> {
    
    @Override
    // map to filter model object
    public CustomNumberFilterModel recognizeFilterModel(Map<String, Object> map) {
        CustomNumberFilterModel customNumberFilter = new CustomNumberFilterModel();
        customNumberFilter.setValue(map.get("value").toString());
        return customNumberFilter;
    }

    @Override
    // default params with default values
    public CustomNumberFilterParams getDefaultFilterParams() {
        return new CustomNumberFilterParams();
    }

    @Override
    // create predicate from expression
    public Predicate toPredicate(CriteriaBuilder cb, Expression<?> expression, CustomNumberFilterModel customNumberFilterModel) {
        String value = customNumberFilterModel.getValue();
        if (value == null || value.equalsIgnoreCase("All")) {
            // not active, always true
            return cb.and();
        }

        Expression<Integer> integerExpression = expression.as(Integer.class);
        Predicate predicate;
        if (value.equalsIgnoreCase("Even")) {
            predicate = cb.equal(cb.mod(integerExpression, 2), 0);
        } else {
            predicate = cb.notEqual(cb.mod(integerExpression, 2), 0);
        }
        
        // predicate also depends on filter params
        if (this.filterParams.isIncludeNullValues()) {
            predicate = cb.or(predicate, cb.isNull(expression));
        }
        
        return predicate;
    }
}
```

### 4. Using your custom filter in ColDefs
Set your filter with filter params to column definition.
```java
boolean includeNullValues = true;

ColDef colDef = ColDef.builder()
    .field("tradeId")
    .filter(
        new CustomNumberFilter()
            .filterParams(new CustomNumberFilterParams(includeNullValues))
    )
    .build();
```


## Grid using Server Side Custom Filter
- `Trade ID` uses custom filter
- `Submitter Deal Id` uses custom filter with option `includeNullValues=true`
- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/filtering/column-filter/custom-filter-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/CustomFilterService.java)

import ShowSqlMonitor from './../../show-sql-monitor';
import CustomFilterGrid from './custom-filter-grid';
import LazyGrid from '../../lazy-grid';

<ShowSqlMonitor serviceUrls={['/docs/filtering/column-filter/custom-filter/getRows']}>
<LazyGrid>
<CustomFilterGrid></CustomFilterGrid>
</LazyGrid>
</ShowSqlMonitor>