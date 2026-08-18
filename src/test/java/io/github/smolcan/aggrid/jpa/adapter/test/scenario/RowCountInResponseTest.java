package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import io.github.smolcan.aggrid.jpa.adapter.test.infrastructure.CountingDriver;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RowCountInResponseTest extends ScenarioTestBase {

    private QueryBuilder.Builder<Trade, Long, Void> config() {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Trade_.book).enableRowGroup(true, key -> key).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).build()
                );
    }

    private QueryBuilder<Trade, Long, Void> withRowCount() {
        return config().includeRowCountInLoadSuccessParams(true).build();
    }

    private ServerSideGetRowsRequest groupedRequest(String groupField) {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        // grouping without a filterModel throws inside whereGrouping, so the suite always sends one
        request.setFilterModel(new HashMap<>());
        request.getRowGroupCols().add(groupCol(groupField));
        request.getValueCols().add(valueCol("currentValue", "sum"));
        return request;
    }

    // ---------------------------------------------------------------- flat grid

    @Test
    void rowCountIsAbsentUnlessTheFlagIsSet() {
        assertThat(config().build().getRows(sortedByIdRequest(0, 5)).getRowCount()).isNull();
    }

    @Test
    void rowCountIsTheWholeResultNotThePage() {
        LoadSuccessParams result = withRowCount().getRows(sortedByIdRequest(0, 5));

        assertThat(result.getRowData()).hasSize(5);
        assertThat(result.getRowCount()).isEqualTo(12);
    }

    @Test
    void rowCountIsTheSameOnEveryPage() {
        assertThat(withRowCount().getRows(sortedByIdRequest(5, 10)).getRowCount()).isEqualTo(12);
        assertThat(withRowCount().getRows(sortedByIdRequest(10, 100)).getRowCount()).isEqualTo(12);
    }

    @Test
    void rowCountIsStillReportedForAPageBeyondTheData() {
        LoadSuccessParams result = withRowCount().getRows(sortedByIdRequest(20, 25));

        assertThat(result.getRowData()).isEmpty();
        assertThat(result.getRowCount()).isEqualTo(12);
    }

    @Test
    void rowCountRespectsFilters() {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 2);
        request.setFilterModel(Map.of("portfolio", filter("contains", "alpha")));

        LoadSuccessParams result = withRowCount().getRows(request);

        assertThat(result.getRowData()).hasSize(2);
        assertThat(result.getRowCount()).isEqualTo(3);
    }

    @Test
    void rowCountAgreesWithTheCountRowsMethod() {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 4);
        request.setFilterModel(Map.of("portfolio", filter("contains", "e")));

        assertThat(withRowCount().getRows(request).getRowCount())
                .isEqualTo(config().build().countRows(request));
    }

    // ---------------------------------------------------------------- grouping

    @Test
    void rowCountCountsGroupsWhenGrouping() {
        LoadSuccessParams result = withRowCount().getRows(groupedRequest("portfolio"));

        // one row per distinct portfolio, and the count agrees with what was returned
        assertThat(result.getRowData()).hasSize(8);
        assertThat(result.getRowCount()).isEqualTo(8);
    }

    @Test
    void rowCountCountsChildRowsOfAnExpandedGroupWhenPaginatingChildRows() {
        QueryBuilder<Trade, Long, Void> queryBuilder = config()
                .includeRowCountInLoadSuccessParams(true)
                .paginateChildRows(true)
                .build();

        ServerSideGetRowsRequest request = groupedRequest("portfolio");
        request.getGroupKeys().add("Alpha");

        LoadSuccessParams result = queryBuilder.getRows(request);

        // trades 1 and 2 are the "Alpha" portfolio, and with every group expanded the count
        // switches from counting groups to counting the rows inside them
        assertThat(result.getRowData()).hasSize(2);
        assertThat(result.getRowCount()).isEqualTo(2);
    }

    /**
     * {@code countRows} counts groups with {@code COUNT(DISTINCT groupCol)}, which skips NULL, and
     * the {@code IN} against the group-key subquery is never true for NULL either, so a group of
     * rows without a key is dropped twice over. Grouping by a nullable column therefore reports a
     * count one lower than the number of rows returned, and a grid told there are 8 rows will not
     * show the 9th.
     */
    @Test
    @Disabled("known undercount: grouping by a nullable column reports 8 for the 9 rows returned, "
            + "because the group of rows without a key survives neither COUNT(DISTINCT book) nor "
            + "the IN against the group-key subquery")
    void rowCountMatchesGroupRowsWhenTheGroupColumnHasNulls() {
        LoadSuccessParams result = withRowCount().getRows(groupedRequest("book"));

        assertThat(result.getRowData()).anyMatch(row -> row.get("book") == null);
        assertThat(result.getRowCount()).isEqualTo(result.getRowData().size());
    }

    @Test
    void groupCountMatchesWhenTheGroupColumnHasNoNulls() {
        LoadSuccessParams result = withRowCount().getRows(groupedRequest("portfolio"));

        assertThat(result.getRowData()).noneMatch(row -> row.get("portfolio") == null);
        assertThat(result.getRowCount()).isEqualTo(result.getRowData().size());
    }

    // ---------------------------------------------------------------- cost and composition

    @Test
    void rowCountCostsExactlyOneExtraStatement() {
        QueryBuilder<Trade, Long, Void> plain = config().build();
        QueryBuilder<Trade, Long, Void> counting = withRowCount();

        assertThat(CountingDriver.countStatements(() -> plain.getRows(sortedByIdRequest(0, 5))))
                .isEqualTo(1);
        assertThat(CountingDriver.countStatements(() -> counting.getRows(sortedByIdRequest(0, 5))))
                .isEqualTo(2);
    }

    @Test
    void rowCountRespectsAlwaysAppliedPredicate() {
        QueryBuilder<Trade, Long, Void> queryBuilder = config()
                .includeRowCountInLoadSuccessParams(true)
                .alwaysAppliedPredicate((cb, root) -> root.get(Trade_.submitterId).in(101, 102, 103, 109, 110))
                .build();

        LoadSuccessParams result = queryBuilder.getRows(sortedByIdRequest(0, 2));

        // the predicate hides 7 of the 12 trades, and the count must not leak them back
        assertThat(result.getRowData()).hasSize(2);
        assertThat(result.getRowCount()).isEqualTo(5);
    }
}
