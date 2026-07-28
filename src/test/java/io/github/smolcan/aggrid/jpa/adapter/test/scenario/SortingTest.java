package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.exceptions.InvalidRequestException;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortDirection;
import io.github.smolcan.aggrid.jpa.adapter.request.SortModelItem;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortingTest extends ScenarioTestBase {

    @Test
    void sortsAscendingByNumberColumn() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getSortModel().add(sortItem("currentValue", SortDirection.asc));

        LoadSuccessParams result = defaultQueryBuilder().getRows(request);

        List<BigDecimal> values = result.getRowData().stream()
                .map(row -> (BigDecimal) row.get("currentValue"))
                .collect(Collectors.toList());
        assertThat(values).hasSize(12).isSortedAccordingTo(Comparator.naturalOrder());
    }

    @Test
    void sortsDescendingByNumberColumn() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getSortModel().add(sortItem("currentValue", SortDirection.desc));

        LoadSuccessParams result = defaultQueryBuilder().getRows(request);

        List<BigDecimal> values = result.getRowData().stream()
                .map(row -> (BigDecimal) row.get("currentValue"))
                .collect(Collectors.toList());
        assertThat(values).hasSize(12).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void multiColumnSortBreaksTies() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getSortModel().add(sortItem("currentValue", SortDirection.asc));
        request.getSortModel().add(sortItem("tradeId", SortDirection.desc));

        LoadSuccessParams result = defaultQueryBuilder().getRows(request);
        // currentValue ascending; the 100.00 tie between 1 and 11 resolved by tradeId descending
        assertThat(tradeIds(result)).containsExactly(3L, 7L, 4L, 12L, 8L, 11L, 1L, 9L, 2L, 6L, 5L, 10L);
    }

    @Test
    void sortsByNestedPath() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getSortModel().add(sortItem("product.name", SortDirection.asc));
        request.getSortModel().add(sortItem("tradeId", SortDirection.asc));

        LoadSuccessParams result = defaultQueryBuilder().getRows(request);
        // H2 sorts nulls first ascending: null product (10), then Gold, Platinum, Silver
        assertThat(tradeIds(result)).containsExactly(10L, 1L, 3L, 6L, 9L, 4L, 7L, 11L, 2L, 5L, 8L, 12L);
    }

    @Test
    void absoluteSortOrdersByMagnitude() {
        SortModelItem absoluteSort = sortItem("currentValue", SortDirection.asc);
        absoluteSort.setType("absolute");

        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getSortModel().add(absoluteSort);
        request.getSortModel().add(sortItem("tradeId", SortDirection.asc));

        LoadSuccessParams result = defaultQueryBuilder().getRows(request);
        // |0.00| |10.00| |42.42| |75.25|x2 |100.00|x2 |150.00| |250.50| |320.10| |500.00| |999.99|
        assertThat(tradeIds(result)).containsExactly(4L, 7L, 12L, 3L, 8L, 1L, 11L, 9L, 2L, 6L, 5L, 10L);
    }

    @Test
    void rejectsUnknownSortColumn() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getSortModel().add(sortItem("notAColumn", SortDirection.asc));

        assertThatThrownBy(() -> defaultQueryBuilder().getRows(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("notAColumn");
    }

    @Test
    void rejectsSortOnNonSortableColumn() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).sortable(false).build()
                )
                .build();

        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getSortModel().add(sortItem("portfolio", SortDirection.asc));

        assertThatThrownBy(() -> queryBuilder.getRows(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("portfolio");
    }

    @Test
    void sortingAppliesTogetherWithFilter() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setFilterModel(Map.of("portfolio", filter("contains", "a")));
        request.getSortModel().add(sortItem("currentValue", SortDirection.desc));

        LoadSuccessParams result = defaultQueryBuilder().getRows(request);
        // portfolios containing 'a' (case-insensitive): 1-10; sorted by currentValue descending
        assertThat(tradeIds(result)).containsExactly(10L, 5L, 6L, 2L, 9L, 1L, 8L, 4L, 7L, 3L);
    }
}
