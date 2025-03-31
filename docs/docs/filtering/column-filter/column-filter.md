---
sidebar_position: 4
---

# Column Filter

Column Filters are filters that are applied to the data at the column level.

## Column Filter Types
You can use the [Provided Filters](https://github.com/smolcan/ag-grid-jpa-adapter/tree/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided) that come with the grid, or you can build your own custom Filters.

There are four main [Provided Filters](https://github.com/smolcan/ag-grid-jpa-adapter/tree/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided), plus the [Multi Filter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/AgMultiColumnFilter.java). These are as follows:
- [Text Filter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/simple/AgTextColumnFilter.java) - A filter for string comparisons.
- [Number Filter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/simple/AgNumberColumnFilter.java) - A filter for number comparisons.
- [Date Filter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/simple/AgDateColumnFilter.java) - A filter for date comparisons.
- [Set Filter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/AgSetColumnFilter.java) -  A filter influenced by how filters work in Microsoft Excel.
- [Multi Filter](https://github.com/smolcan/ag-grid-jpa-adapter/blob/main/src/main/java/io/github/smolcan/aggrid/jpa/adapter/filter/provided/AgMultiColumnFilter.java) - Allows for multiple column filters to be used together.