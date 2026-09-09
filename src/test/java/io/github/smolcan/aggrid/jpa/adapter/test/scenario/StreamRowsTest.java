package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.exceptions.InvalidRequestException;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgNumberColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder.MasterDetailParams;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortDirection;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Employee;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Employee_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import io.github.smolcan.aggrid.jpa.adapter.test.infrastructure.CountingDriver;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Streaming the whole result set for exports, past the block the request asks for. */
class StreamRowsTest extends ScenarioTestBase {

    private static List<Long> tradeIds(Stream<Map<String, Object>> rows) {
        return rows.map(row -> ((Number) row.get("tradeId")).longValue()).collect(Collectors.toList());
    }

    private ServerSideGetRowsRequest groupedByPortfolioRequest() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setFilterModel(new HashMap<>());
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getValueCols().add(valueCol("currentValue", "sum"));
        return request;
    }

    private QueryBuilder<Trade, Long, Void> groupingQueryBuilder() {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).filter(new AgNumberColumnFilter<>()).build()
                )
                .build();
    }

    private QueryBuilder<Employee, Long, Void> treeQueryBuilder() {
        return QueryBuilder.builder(Employee.class, Employee_.employeeId, entityManager)
                .colDefs(
                        ColDef.builder(Employee_.employeeId).build(),
                        ColDef.builder(Employee_.name).filter(new AgTextColumnFilter()).build()
                )
                .treeData(true)
                .isServerSideGroupFieldName("isGroup")
                .treeDataStringToParentIdTypeConverter(Long::valueOf)
                .treeDataDataPathFieldName(Employee_.path)
                .treeDataDataPathSeparator("/")
                .treeDataParentReferenceField(Employee_.manager)
                .build();
    }

    @Test
    void streamsEveryRowNotJustTheRequestedBlock() {
        // the window the grid asked for holds two rows, the export holds all twelve
        List<Long> ids = tradeIds(defaultQueryBuilder().streamRows(sortedByIdRequest(0, 2)));

        assertThat(ids).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
    }

    @Test
    void chunksNeitherRepeatNorSkipRows() {
        // portfolio has duplicates, so the sort model alone leaves the order of the tied rows open
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getSortModel().add(sortItem("portfolio", SortDirection.asc));

        List<Map<String, Object>> rows = defaultQueryBuilder().streamRows(request, 5).collect(Collectors.toList());

        assertThat(rows).extracting(row -> ((Number) row.get("tradeId")).longValue())
                .containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
        // the tiebreaker orders rows of the same portfolio by trade id, which is what keeps
        // the chunks from overlapping in the first place
        Map<Object, List<Long>> idsPerPortfolio = rows.stream().collect(Collectors.groupingBy(
                row -> row.get("portfolio"),
                Collectors.mapping(row -> ((Number) row.get("tradeId")).longValue(), Collectors.toList())));
        assertThat(idsPerPortfolio.values()).allSatisfy(ids -> assertThat(ids).isSorted());
    }

    @Test
    void sortModelIsKept() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getSortModel().add(sortItem("tradeId", SortDirection.desc));

        List<Long> ids = tradeIds(defaultQueryBuilder().streamRows(request, 5));

        assertThat(ids).containsExactly(12L, 11L, 10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L);
    }

    @Test
    void filtersApply() {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("portfolio", filter("equals", "Gamma")));

        assertThat(tradeIds(defaultQueryBuilder().streamRows(request, 1))).containsExactly(7L, 8L);
    }

    @Test
    void alwaysAppliedPredicateApplies() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).build()
                )
                .alwaysAppliedPredicate((cb, root) -> cb.equal(root.get(Trade_.portfolio), "Gamma"))
                .build();

        assertThat(tradeIds(queryBuilder.streamRows(emptyRequest(0, 100), 1))).containsExactly(7L, 8L);
    }

    @Test
    void streamsGroupRowsWhenGrouping() {
        List<Map<String, Object>> rows = groupingQueryBuilder()
                .streamRows(groupedByPortfolioRequest(), 3)
                .collect(Collectors.toList());

        assertThat(rows).extracting(row -> row.get("portfolio"))
                .containsExactlyInAnyOrder("Alpha", "alpha", "Beta", "BETA", "Gamma", "delta", "Delta", "Epsilon");
        assertThat(rows).allSatisfy(row -> assertThat(row).containsKey("currentValue"));
    }

    @Test
    void streamsTreeDataLevel() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getSortModel().add(sortItem("employeeId", SortDirection.asc));

        List<Map<String, Object>> rows = treeQueryBuilder().streamRows(request, 2).collect(Collectors.toList());

        assertThat(rows).extracting(row -> ((Number) row.get("employeeId")).longValue())
                .containsExactly(1L, 8L, 10L);
        assertThat(rows).extracting(row -> row.get("isGroup")).containsExactly(true, true, false);
    }

    @Test
    void masterDetailRowsKeepTheirDetailsInEveryChunk() {
        QueryBuilder<Product, Long, Trade> queryBuilder = QueryBuilder.builder(Product.class, Product_.productId, Trade.class, entityManager)
                .colDefs(
                        ColDef.builder(Product_.productId).build(),
                        ColDef.builder(Product_.name).build()
                )
                .masterDetailParams(MasterDetailParams.<Product, Long, Trade>builder()
                        .detailClass(Trade.class)
                        .detailColDefs(ColDef.builder(Trade_.tradeId).build())
                        .detailMasterReferenceField(Trade_.product)
                        .build())
                .masterDetailLazy(false)
                .masterDetailRowDataFieldName("detailRows")
                .build();

        // three products over chunks of two, so the second chunk has to attach details of its own
        List<Map<String, Object>> rows = queryBuilder.streamRows(emptyRequest(0, 100), 2).collect(Collectors.toList());

        assertThat(rows).hasSize(3);
        assertThat(rows).allSatisfy(row -> assertThat((List<?>) row.get("detailRows")).isNotEmpty());
    }

    @Test
    void oneQueryPerChunk() {
        QueryBuilder<Trade, Long, Void> queryBuilder = defaultQueryBuilder();

        // 12 rows in chunks of 5: the third chunk comes back short and ends the stream
        long shortLastChunk = CountingDriver.countStatements(
                () -> queryBuilder.streamRows(sortedByIdRequest(0, 100), 5).forEach(row -> { }));
        assertThat(shortLastChunk).isEqualTo(3);

        // 12 rows in chunks of 4: the third chunk is full, so it takes one more query to learn there is no fourth
        long fullLastChunk = CountingDriver.countStatements(
                () -> queryBuilder.streamRows(sortedByIdRequest(0, 100), 4).forEach(row -> { }));
        assertThat(fullLastChunk).isEqualTo(4);
    }

    @Test
    void chunksAreQueriedOnlyAsTheyAreConsumed() {
        QueryBuilder<Trade, Long, Void> queryBuilder = defaultQueryBuilder();

        long statements = CountingDriver.countStatements(
                () -> queryBuilder.streamRows(sortedByIdRequest(0, 100), 5).limit(2).forEach(row -> { }));

        assertThat(statements).isEqualTo(1);
    }

    @Test
    void chunkSizeMustBePositive() {
        QueryBuilder<Trade, Long, Void> queryBuilder = defaultQueryBuilder();

        assertThatThrownBy(() -> queryBuilder.streamRows(sortedByIdRequest(0, 100), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chunk size");
    }

    @Test
    void requestIsValidatedBeforeAnyChunkIsRead() {
        QueryBuilder<Trade, Long, Void> queryBuilder = defaultQueryBuilder();
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getSortModel().add(sortItem("notAColumn", SortDirection.asc));

        assertThatThrownBy(() -> queryBuilder.streamRows(request))
                .isInstanceOf(InvalidRequestException.class);
    }
}
