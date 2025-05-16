---
sidebar_position: 8
---

# Pagination
When using pagination for the grid, you need to provide the total row count through a separate request to the backend. 

Only make this request when grid state changes in a way that affects the total count (like filter changes).
For grids with grouping enabled, only root groups are counted. 

Use the `queryBuilder.countRows(request)` method to retrieve this count

## Paginate child rows
! WIP !
