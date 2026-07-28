package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortDirection;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationTest extends ScenarioTestBase {

    @Test
    void returnsAllRowsWhenPageCoversWholeDataset() {
        LoadSuccessParams result = defaultQueryBuilder().getRows(sortedByIdRequest(0, 100));
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
    }

    @Test
    void returnsFirstPage() {
        LoadSuccessParams result = defaultQueryBuilder().getRows(sortedByIdRequest(0, 5));
        assertThat(tradeIds(result)).containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void returnsMiddlePage() {
        LoadSuccessParams result = defaultQueryBuilder().getRows(sortedByIdRequest(5, 10));
        assertThat(tradeIds(result)).containsExactly(6L, 7L, 8L, 9L, 10L);
    }

    @Test
    void returnsPartialLastPage() {
        LoadSuccessParams result = defaultQueryBuilder().getRows(sortedByIdRequest(10, 100));
        assertThat(tradeIds(result)).containsExactly(11L, 12L);
    }

    @Test
    void returnsEmptyPageBeyondData() {
        LoadSuccessParams result = defaultQueryBuilder().getRows(sortedByIdRequest(20, 25));
        assertThat(result.getRowData()).isEmpty();
    }

    @Test
    void pageWindowAppliesAfterSorting() {
        ServerSideGetRowsRequest request = emptyRequest(0, 3);
        request.getSortModel().add(sortItem("currentValue", SortDirection.desc));

        LoadSuccessParams result = defaultQueryBuilder().getRows(request);
        // three highest currentValues: 999.99 (10), 500.00 (5), 320.10 (6)
        assertThat(tradeIds(result)).containsExactly(10L, 5L, 6L);
    }

    @Test
    void countsAllRows() {
        long count = defaultQueryBuilder().countRows(emptyRequest(0, 5));
        assertThat(count).isEqualTo(12);
    }

    @Test
    void countIgnoresPaginationButAppliesFilter() {
        ServerSideGetRowsRequest request = emptyRequest(0, 2);
        request.setFilterModel(Map.of("portfolio", filter("contains", "alpha")));

        long count = defaultQueryBuilder().countRows(request);
        assertThat(count).isEqualTo(3);
    }
}
