---
sidebar_position: 7
---

# Quick Filter

Filter rows by comparing against the data in specified columns.

:::warning
This feature is officially not supported for server-side row model in Official Ag-Grid documentation
:::

## Configuration

To enable quick filtering, you must set `.isQuickFilterPresent(true)` and define which fields should be searched using `.quickFilterSearchInFields()`.

You can also fine-tune text handling with optional parameters like trimming, case sensitivity, or custom formatters.


```java
QueryBuilder.builder(Trade.class, entityManager)
    // 1. Enable Quick Filter support
    .isQuickFilterPresent(true)
    
    // 2. Define fields to search in
    .quickFilterSearchInFields("product", "portfolio")
    
    // 3. Optional configuration
    .quickFilterTrimInput(true)       // Trim spaces from input
    .quickFilterCaseSensitive(false)  // Ignore case (default behavior)
    .quickFilterTextFormatter(...)    // custom text formatting applied before matching
    .build();
```

On the client side, you must pass the search string in the quickFilter field of the request body:

```javascript
fetch('/api/rows', {
    body: JSON.stringify({
        ...params.request,
        quickFilter: quickFilterValue // String from your search input
    }),
    ...
})
```

## Parsing Logic

By default, the adapter splits the input string into individual words using spaces as delimiters. 
This allows for multi-word matching (e.g., searching "Apple Inc" will match rows containing "Apple" AND "Inc" in any of the configured fields).

If you need different behavior (e.g., splitting by commas or keeping the phrase exact), you can provide a custom parser:

```java
.quickFilterParser(input -> Arrays.asList(input.split(","))) // Split by comma
```

## Custom Matching Logic

If the default strategy (checking if words exist in columns using `LIKE %word%`) does not fit your needs, you can provide your own predicate logic using `.quickFilterMatcher()`.

This function receives the CriteriaBuilder, Root, and the list of parsed words and returns Predicate.

```java
.quickFilterTextFormatter((cb, stringExpr) -> {
                    Expression<String> newExpression = stringExpr;
                    // Remove accents
                    newExpression = cb.function("TRANSLATE", String.class, newExpression,
                            cb.literal("áéíóúÁÉÍÓÚüÜñÑ"),
                            cb.literal("aeiouAEIOUuUnN"));

                    return newExpression;
                })
```


## Example

- Source code for this grid available [here](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/docs/docs/filtering/quick-filter-grid.tsx)
- Backend source code available [here](https://github.com/smolcan/ag-grid-jpa-adapter-docs-backend/blob/main/src/main/java/io/github/smolcan/ag_grid_jpa_adapter_docs_backend/service/docs/QuickFilterService.java)

import GridLoadingMessage from './../grid-loading-message';
import QuickFilterGrid from './quick-filter-grid';

<GridLoadingMessage>
<QuickFilterGrid></QuickFilterGrid>
</GridLoadingMessage>