---
sidebar_position: 4
---

# Column Filter

Column Filters are filters that are applied to the data at the column level.

## Column Filter Types
You can use the [Provided Filters](https://github.com/smolcan/ag-grid-jpa-adapter/tree/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided) that come with the grid, or you can build your own custom Filters.

There are four main [Provided Filters](https://github.com/smolcan/ag-grid-jpa-adapter/tree/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided), plus the [Multi Filter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/AgMultiColumnFilter.java). These are as follows:
- [Text Filter](text-filter.md) - A filter for string comparisons.
- [Number Filter](number-filter.md) - A filter for number comparisons.
- [Date Filter](date-filter.md) - A filter for date comparisons.
- [Set Filter](set-filter.md) -  A filter influenced by how filters work in Microsoft Excel.
- [Multi Filter](multi-filter.md) - Allows for multiple column filters to be used together.

You can also define you own [Custom Filter](custom-filter.md)
