---
sidebar_position: 11
---

# Grand Total Row

import ShowSqlMonitor from './show-sql-monitor';
import LazyGrid from './lazy-grid';
import GrandTotalRowGrid from './grand-total-row-grid';
import GrandTotalRowAsyncGrid from './grand-total-row-async-grid';

<ShowSqlMonitor serviceUrls={['/docs/grand-total-row/getRows', '/docs/grand-total-row/supplySetFilterValues']}>
<LazyGrid>
<GrandTotalRowGrid></GrandTotalRowGrid>
</LazyGrid>
</ShowSqlMonitor>


<ShowSqlMonitor serviceUrls={['/docs/grand-total-row/async/getRows', '/docs/grand-total-row/async/supplySetFilterValues', '/docs/grand-total-row/async/getGrandTotalData']}>
<LazyGrid>
<GrandTotalRowAsyncGrid></GrandTotalRowAsyncGrid>
</LazyGrid>
</ShowSqlMonitor>
