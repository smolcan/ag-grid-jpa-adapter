---
sidebar_position: 13
---

# Compatibility

Every cell below runs the full test suite against that JPA provider and database.

import CompatibilityMatrix from './compatibility-matrix';

<CompatibilityMatrix />

Releases between the tested ones that implement the same Jakarta Persistence version are expected to work, but are
not tested.

The JPA 2.2 rows test the `javax` jar, the same library for `javax.persistence`:

```xml
<dependency>
    <groupId>io.github.smolcan</groupId>
    <artifactId>ag-grid-jpa-adapter</artifactId>
    <version>${ag-grid-jpa-adapter.version}</version>
    <classifier>javax</classifier>
</dependency>
```

## Known issues

All of them are in EclipseLink and come from how it maps or binds values, not from the queries the adapter builds.

### A. Grouping by a `CASE` over strings {#eclipselink-case-grouping}

EclipseLink binds the literals of a [computed column](./columns.md#computed-columns) separately in `SELECT`,
`GROUP BY` and `ORDER BY`, so the database cannot tell the grouped expression is the selected one:

```sql
SELECT CASE WHEN (CURRENTVALUE > ?) THEN ? ELSE ? END, SUM(CURRENTVALUE) FROM TRADE
GROUP BY CASE WHEN (CURRENTVALUE > ?) THEN ? ELSE ? END
-- ERROR: column "trade.currentvalue" must appear in the GROUP BY clause
```

Compute the value in the database instead, in a generated column or a view, and map it as a regular attribute.

:::danger
Do not work around it with `eclipselink.jdbc.bind-parameters=false` on MySQL or MariaDB. EclipseLink then writes the
filter text into the SQL without escaping backslashes, which makes the text filters injectable.
:::

### B. Time zone types on Oracle and SQL Server {#eclipselink-time-zone-types}

Columns of `OffsetDateTime`, `ZonedDateTime` and `Instant`:

- Oracle: EclipseLink cannot read `TIMESTAMP WITH TIME ZONE` values back (`ConversionException` on
  `oracle.sql.TIMESTAMPTZ`), so every query selecting such a column fails. The test's `java.util.Date`, `Timestamp`
  and `java.sql.Date` cases fail with it, because they share a grid with those columns.
- SQL Server: EclipseLink fails to read and compare `DATETIMEOFFSET` values.
- `Instant` became a standard JPA type in 3.2. EclipseLink 2.7, 3.0 and 4.0 store it as a serialized BLOB, which
  neither database can compare.

`LocalDate` and `LocalDateTime` columns work on every combination.

### C. EclipseLink 4.0: rows with a `NULL` UUID {#eclipselink-4-null-uuid}

EclipseLink 4.0's `UUIDConverter` throws a `NullPointerException` when it reads a `NULL` UUID, so every query
returning such a row fails. EclipseLink 3.0 and 5.0 are not affected.

### D. EclipseLink 2.7 and 3.0: comparing UUIDs on Oracle and SQL Server {#eclipselink-3-uuid-comparison}

`UUID` became a standard JPA type in 3.1. EclipseLink 2.7 and 3.0 store it as a serialized BLOB, which neither
database can compare, so filtering a UUID column fails. An `AttributeConverter` that stores the UUID as a string
avoids it.

### E. EclipseLink 5.0 on SQL Server: the suite does not start {#eclipselink-5-sql-server-uuid}

Inserting the test data fails: EclipseLink 5.0 binds UUIDs as strings into the binary column it generates for them,
and SQL Server rejects the implicit conversion. Until the test schema works around it, this combination is untested.

### Skipped: row count when grouping by a nullable column {#null-group-row-count}

One test is skipped on every combination. With row grouping on a column that holds `NULL`s, the row count reports one
group less than the rows returned. This is a bug in the adapter.
