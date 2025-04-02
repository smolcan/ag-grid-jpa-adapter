---
sidebar_position: 1
---

# Text Filter
Text Filters allow you to filter string data.

## Using Text Filter
Text filter is represented by class [AgTextColumnFilter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/simple/AgTextColumnFilter.java).

Text Filter is the default filter used in `ColDef`, but it can also be explicitly configured as shown below.
```java
ColDef colDef = ColDef.builder()
    .field("portfolio")
    .filter(new AgTextColumnFilter())
    .build()
```

## Text Filter Parameters
Text Filters are configured though the filter params ([TextFilterParams](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/model/simple/params/TextFilterParams.java) class)

| Property                      | Type                                                                  | Default    | Description                                                                                                                                                                                      |
|-------------------------------|-----------------------------------------------------------------------|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`textMatcher`** | `BiFunction<CriteriaBuilder, TextMatcherParams, Predicate>`           | —           | Used to override how to filter based on the user input. Returns true if the value passes the filter, otherwise false.                                                                            |
| **`caseSensitive`**                | `boolean`                                                             | `false`    | By default, text filtering is case-insensitive. Set this to true to make text filtering case-sensitive.                                                                                          |
| **`textFormatter`**                  | `BiFunction<CriteriaBuilder, Expression<String>, Expression<String>>` | —           | Formats the text before applying the filter compare logic. Useful if you want to substitute accented characters, for example.                                                                    |
| **`trimInput`**         | `boolean`                                                             | `false`    | If true, the input that the user enters will be trimmed when the filter is applied, so any leading or trailing whitespace will be removed. If only whitespace is entered, it will be left as-is. |
| **`filterOptions`**         | `Set<SimpleFilterModelType>`                                            | All available | Which filtering operations are allowed.                                                                                                                                                          |

Example of using filter parameters.
```java
ColDef colDef = ColDef.builder()
    .field("portfolio")
    .filter(new AgTextColumnFilter()
        .filterParams(
            TextFilterParams.builder()
                .caseSensitive(false)
                .trimInput(true)
                .filterOptions(SimpleFilterModelType.contains, SimpleFilterModelType.startsWith)
                .build()
        )
    )
    .build()
```

## Text Formatter
By default, the grid compares the Text Filter with the values in a case-insensitive way, by converting both the filter text and the values to lower case and comparing them; for example, `'o'` will match `'Olivia'` and `'Salmon'`. 
If you instead want to have case-sensitive matches, you can set `caseSensitive = true` in the `filterParams`, so that no lowercasing is performed. 
In this case, `'o'` would no longer match `'Olivia'`.

You might have more advanced requirements, for example to ignore accented characters.
In this case, you can provide your own `textFormatter`, which formats the text before applying the filter compare logic.
`textFormatter` is a function, that takes as argument `CriteriaBuilder` object and `Expression<String>` and returns new `Expression<String>` which is then used
in the filter compare logic.

```java
ColDef colDef = ColDef.builder()
    .field("portfolio")
    .filter(new AgTextColumnFilter()
        .filterParams(
            TextFilterParams.builder()
                .textFormatter((cb, expr) => {
                    Expression<String> newExpression = expr;
                    // lower input
                    newExpression = cb.lower(newExpression);
                    // Remove accents
                    newExpression = cb.function("TRANSLATE", String.class, newExpression,
                        cb.literal("áéíóúÁÉÍÓÚüÜñÑ"),
                        cb.literal("aeiouAEIOUuUnN"));
                    
                    return newExpression;
                })
                .build()
        )
    )
    .build()
```
Note that when providing a Text Formatter, the `caseSensitive` parameter is ignored. 
In this situation, if you want to do a case-insensitive comparison, you will need to perform case conversion inside the `textFormatter` function.

## Text Custom Matcher

## Text Filter Model
