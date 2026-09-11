---
sidebar_position: 12
---

# Export
The adapter does not produce CSV or Excel files. Column order, header names, formatting and the file format
are the application's decisions, so the adapter only gives you the rows and you write the file yourself.

`streamRows` applies everything in the request the same way `getRows` does — filters, grouping, group keys,
sorting — and ignores `startRow` and `endRow`, so you get every row the grid would show across all its blocks:

```java
@Transactional(readOnly = true)
public void writeCsv(ServerSideGetRowsRequest request, Writer writer) {
    this.queryBuilder.streamRows(request).forEach(row -> writeLine(writer, row));
}
```

Rows are read in chunks, one query per chunk, so the whole result set is never held in memory at once.
The default chunk size is `1000` and can be set on the builder, or per call:

```java
QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
    .colDefs(...)
    .streamChunkSize(5_000)
    .build();

queryBuilder.streamRows(request, 5_000);
```

Chunks are paged by offset, so the rows are ordered by the request's sort model plus the primary key (or the
group columns when the rows are grouped). Without that, rows with equal sort values could repeat in one chunk
and be missing from the next.
