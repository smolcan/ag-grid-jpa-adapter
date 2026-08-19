package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.exceptions.OnPivotMaxColumnsExceededException;
import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgNumberColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortDirection;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Employee;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Employee_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlwaysAppliedPredicateTest extends ScenarioTestBase {

    private static final List<Integer> VISIBLE_SUBMITTERS = List.of(101, 102, 103, 109, 110);
    private static final List<Long> VISIBLE_TRADES = List.of(1L, 2L, 3L, 9L, 10L);

    private QueryBuilder.Builder<Trade, Long, Void> tradeConfig() {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Trade_.book).enableRowGroup(true, key -> key).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).filter(new AgNumberColumnFilter<>()).build(),
                        ColDef.builder(FieldPath.of(Trade_.product).to(Product_.name)).enablePivot(true).build()
                );
    }

    /**
     * Restricted to the submitters above, the way a row-level security rule would be. Any provider
     * renders an integer {@code IN} list the same way, which a boolean comparison is not: EclipseLink
     * inlines {@code true} as {@code 1} and H2 then refuses to compare it against a BOOLEAN column.
     */
    private QueryBuilder<Trade, Long, Void> restricted() {
        return restrictedConfig().build();
    }

    private QueryBuilder.Builder<Trade, Long, Void> restrictedConfig() {
        return tradeConfig().alwaysAppliedPredicate((cb, root) -> root.get(Trade_.submitterId).in(VISIBLE_SUBMITTERS));
    }

    /** Same columns, no predicate, so the tests can state what the restriction actually changed. */
    private QueryBuilder<Trade, Long, Void> unrestricted() {
        return tradeConfig().build();
    }

    private ServerSideGetRowsRequest groupedByBookRequest() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        // grouping without a filterModel throws inside whereGrouping, so the suite always sends one
        request.setFilterModel(new HashMap<>());
        request.getRowGroupCols().add(groupCol("book"));
        request.getValueCols().add(valueCol("currentValue", "sum"));
        request.getSortModel().add(sortItem("book", SortDirection.asc));
        return request;
    }

    // ---------------------------------------------------------------- basic grid

    @Test
    void restrictsRowsOnPlainGrid() {
        assertThat(tradeIds(restricted().getRows(sortedByIdRequest(0, 100))))
                .containsExactlyElementsOf(VISIBLE_TRADES);
    }

    @Test
    void withoutPredicateAllRowsAreReturned() {
        assertThat(tradeIds(unrestricted().getRows(sortedByIdRequest(0, 100))))
                .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
    }

    @Test
    void combinesWithColumnFilterUsingAnd() {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("portfolio", filter("contains", "alpha")));

        assertThat(tradeIds(restricted().getRows(request))).containsExactly(1L, 2L, 3L);
    }

    @Test
    void columnFilterCannotReachRowsThePredicateHides() {
        // every "Beta" trade (4, 5, 6) belongs to another submitter, so the filter matches nothing
        // that survives the predicate
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("portfolio", filter("contains", "beta")));

        assertThat(unrestricted().getRows(request).getRowData()).hasSize(3);
        assertThat(restricted().getRows(request).getRowData()).isEmpty();
    }

    @Test
    void appliesBeforePaginationSoPagesHoldOnlyVisibleRows() {
        // the second page of the restricted result, not the restricted view of page two
        assertThat(tradeIds(restricted().getRows(sortedByIdRequest(2, 4)))).containsExactly(3L, 9L);
    }

    @Test
    void sortingRunsOverTheRestrictedSetOnly() {
        ServerSideGetRowsRequest request = emptyRequest(0, 1);
        request.getSortModel().add(sortItem("currentValue", SortDirection.desc));

        // 999.99 (trade 10) is the highest visible value; 500.00 (trade 5) is higher but hidden
        assertThat(tradeIds(restricted().getRows(request))).containsExactly(10L);
    }

    // ---------------------------------------------------------------- counting

    @Test
    void countRowsCountsOnlyVisibleRows() {
        assertThat(restricted().countRows(emptyRequest(0, 100))).isEqualTo(5);
        assertThat(unrestricted().countRows(emptyRequest(0, 100))).isEqualTo(12);
    }

    @Test
    void countRowsCombinesPredicateWithFilter() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setFilterModel(Map.of("portfolio", filter("contains", "alpha")));

        assertThat(restricted().countRows(request)).isEqualTo(3);
    }

    @Test
    void countRowsCountsOnlyGroupsThatHaveVisibleRows() {
        // visible trades leave books B-1, B-2, B-5 and the null group, and counting distinct
        // group values skips the null one; unrestricted the same query reaches every book
        assertThat(restricted().countRows(groupedByBookRequest())).isEqualTo(3);
        assertThat(unrestricted().countRows(groupedByBookRequest()))
                .isGreaterThan(restricted().countRows(groupedByBookRequest()));
    }

    // ---------------------------------------------------------------- grouping and aggregation

    @Test
    void groupsCollapseToVisibleRowsOnly() {
        LoadSuccessParams result = restricted().getRows(groupedByBookRequest());

        assertThat(columnValues(result, "book")).containsExactlyInAnyOrder("B-1", "B-2", "B-5", null);
    }

    @Test
    void aggregatesIgnoreRowsThePredicateHides() {
        LoadSuccessParams result = restricted().getRows(groupedByBookRequest());

        // book B-1 holds trade 1 (100.00, visible) and trade 6 (320.10, hidden)
        assertThat(sumForBook(result, "B-1")).isEqualTo(100.00);
        assertThat(sumForBook(unrestricted().getRows(groupedByBookRequest()), "B-1")).isEqualTo(420.10);
    }

    @Test
    void drillingIntoGroupKeepsChildRowsRestricted() {
        ServerSideGetRowsRequest request = groupedByBookRequest();
        request.getGroupKeys().add("B-1");
        request.getSortModel().clear();
        request.getSortModel().add(sortItem("tradeId", SortDirection.asc));

        // trade 6 shares book B-1 but belongs to another submitter
        assertThat(tradeIds(restricted().getRows(request))).containsExactly(1L);
    }

    // ---------------------------------------------------------------- group aggregate filtering

    private QueryBuilder<Trade, Long, Void> restrictedGroupAggFiltering() {
        return restrictedConfig().groupAggFiltering(true).build();
    }

    private QueryBuilder<Trade, Long, Void> unrestrictedGroupAggFiltering() {
        return tradeConfig().groupAggFiltering(true).build();
    }

    /** Grouped by book, keeping groups whose summed value clears 300 (or holding a leaf that does). */
    private ServerSideGetRowsRequest groupAggFilteringRequest(String... groupKeys) {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getRowGroupCols().add(groupCol("book"));
        request.getValueCols().add(valueCol("currentValue", "sum"));
        request.setFilterModel(Map.of("currentValue", filter("greaterThan", 300)));
        request.getSortModel().add(sortItem("book", SortDirection.asc));
        request.getGroupKeys().addAll(List.of(groupKeys));
        return request;
    }

    @Test
    void groupAggFilteringMatchesAggregatesOfVisibleRowsOnly() {
        // book B-1 holds trade 1 (100.00, visible) and trade 6 (320.10, hidden): for this user
        // neither the group sum nor any leaf clears 300, so B-1 must not surface at all
        assertThat(columnValues(restrictedGroupAggFiltering().getRows(groupAggFilteringRequest()), "book"))
                .containsExactly("B-2");
        assertThat(columnValues(unrestrictedGroupAggFiltering().getRows(groupAggFilteringRequest()), "book"))
                .contains("B-1");
    }

    @Test
    void groupAggFilteringExpandedParentMatchesOnVisibleRowsOnly() {
        ServerSideGetRowsRequest request = groupAggFilteringRequest("B-1");
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getSortModel().clear();
        request.getSortModel().add(sortItem("portfolio", SortDirection.asc));

        // a matching expanded parent exposes all its child groups, so B-1 passing on hidden rows
        // would hand this user the whole level; restricted, B-1 sums to 100.00 and nothing passes
        assertThat(restrictedGroupAggFiltering().getRows(request).getRowData()).isEmpty();
        assertThat(columnValues(unrestrictedGroupAggFiltering().getRows(request), "portfolio"))
                .containsExactly("Alpha", "BETA");
    }

    // ---------------------------------------------------------------- grand total

    @Test
    void grandTotalSumsOnlyVisibleRows() {
        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setNeedsGrandTotal(true);
        request.getValueCols().add(valueCol("currentValue", "sum"));

        QueryBuilder<Trade, Long, Void> queryBuilder = restrictedConfig().grandTotalRow(true).build();

        // 100.00 + 250.50 - 75.25 + 150.00 + 999.99; the unrestricted total is 2453.01
        assertThat(((Number) queryBuilder.getRows(request).getGrandTotalData().get("currentValue")).doubleValue())
                .isEqualTo(1425.24);
    }

    // ---------------------------------------------------------------- pivoting

    private ServerSideGetRowsRequest pivotRequest() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setPivotMode(true);
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getPivotCols().add(groupCol("product.name"));
        request.getValueCols().add(valueCol("currentValue", "sum"));
        request.getSortModel().add(sortItem("portfolio", SortDirection.asc));
        return request;
    }

    @Test
    void pivotedAggregatesCoverOnlyVisibleRows() {
        LoadSuccessParams result = restricted().getRows(pivotRequest());

        // only Alpha, alpha, Delta and delta hold visible trades; Beta/BETA/Gamma/Epsilon disappear
        assertThat(columnValues(result, "portfolio")).containsExactlyInAnyOrder("Alpha", "alpha", "Delta", "delta");

        Map<String, Object> alpha = rowWhere(result, "portfolio", "Alpha");
        assertThat(((Number) alpha.get("Gold_currentValue")).doubleValue()).isEqualTo(100.00);
        assertThat(((Number) alpha.get("Silver_currentValue")).doubleValue()).isEqualTo(250.50);
    }

    @Test
    void pivotColumnsAreGeneratedOnlyFromVisibleValues() {
        // pivot column headers are the values themselves: Platinum only ever appears in trades 4, 7
        // and 11, all hidden, so generating a column for it would leak it. Trade 10 has no product,
        // which is where the null column comes from.
        assertThat(restricted().getRows(pivotRequest()).getPivotResultFields())
                .containsExactlyInAnyOrder("null_currentValue", "Gold_currentValue", "Silver_currentValue");
        assertThat(unrestricted().getRows(pivotRequest()).getPivotResultFields())
                .contains("Platinum_currentValue");
    }

    @Test
    void pivotMaxColumnsCountsOnlyVisibleValues() {
        // 1 value col x (Gold, Silver) = 2 columns for this user, but 3 across the whole table,
        // so an unrestricted count would refuse a request that is well inside the limit
        assertThatCode(() -> restrictedConfig().pivotMaxGeneratedColumns(2).build().getRows(pivotRequest()))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> tradeConfig().pivotMaxGeneratedColumns(2).build().getRows(pivotRequest()))
                .isInstanceOf(OnPivotMaxColumnsExceededException.class);
    }

    // ---------------------------------------------------------------- tree data

    /**
     * Employees earning at least 150, which leaves 1 Alice, 2 Bob, 3 Carol, 7 Grace, 8 Heidi and
     * 9 Ivan. Bob's and Carol's children all fall below the threshold, and root 10 Judy (130) goes
     * too, so both the root level and the group flags change.
     */
    private QueryBuilder<Employee, Long, Void> restrictedTree() {
        return restrictedTreeConfig().build();
    }

    private QueryBuilder.Builder<Employee, Long, Void> restrictedTreeConfig() {
        return QueryBuilder.builder(Employee.class, Employee_.employeeId, entityManager)
                .colDefs(
                        ColDef.builder(Employee_.employeeId).build(),
                        ColDef.builder(Employee_.name).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Employee_.salary).enableValue(true).build()
                )
                .treeData(true)
                .isServerSideGroupFieldName("isGroup")
                .treeDataParentReferenceField(Employee_.manager)
                .treeDataStringToParentIdTypeConverter(Long::valueOf)
                .treeDataDataPathFieldName(Employee_.path)
                .treeDataDataPathSeparator("/")
                .getChildCount(true)
                .getChildCountFieldName("childCount")
                .alwaysAppliedPredicate((cb, root) -> cb.ge(root.get(Employee_.salary), new BigDecimal("150.00")));
    }

    private ServerSideGetRowsRequest treeRequest(String... groupKeys) {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        for (String key : groupKeys) {
            request.getGroupKeys().add(key);
        }
        request.getSortModel().add(sortItem("employeeId", SortDirection.asc));
        return request;
    }

    @Test
    void treeGroupFlagIsRestrictedInChildrenCollectionModeToo() {
        QueryBuilder<Employee, Long, Void> queryBuilder = restrictedTreeConfig()
                .treeDataChildrenField(Employee_.children)
                .build();

        LoadSuccessParams roots = queryBuilder.getRows(treeRequest());
        assertThat(employeeIds(roots)).containsExactly(1L, 8L);
        assertThat(columnValues(roots, "isGroup")).containsExactly(true, true);

        // Bob and Carol keep children in the table, but none of them is visible
        assertThat(columnValues(queryBuilder.getRows(treeRequest("1")), "isGroup"))
                .containsExactly(false, false, false);
    }

    @Test
    void treeRootLevelHidesRestrictedRoots() {
        // Judy (130.00) is a root but falls below the threshold
        assertThat(employeeIds(restrictedTree().getRows(treeRequest()))).containsExactly(1L, 8L);
    }

    @Test
    void treeDrillDownHidesRestrictedChildren() {
        // Alice's children are Bob (300), Carol (250) and Grace (200): all above the threshold
        assertThat(employeeIds(restrictedTree().getRows(treeRequest("1")))).containsExactly(2L, 3L, 7L);
        // Bob's children Dave (100) and Eve (120) are both below it
        assertThat(employeeIds(restrictedTree().getRows(treeRequest("1", "2")))).isEmpty();
    }

    @Test
    void treeChildCountIgnoresRestrictedChildren() {
        LoadSuccessParams result = restrictedTree().getRows(treeRequest());

        // Alice's visible descendants are Bob, Carol and Grace; Dave, Eve and Frank are hidden
        assertThat(childCountFor(result, 1L)).isEqualTo(3);
        // Heidi keeps Ivan (150.00)
        assertThat(childCountFor(result, 8L)).isEqualTo(1);
    }

    @Test
    void treeGroupFlagIgnoresRestrictedChildren() {
        LoadSuccessParams result = restrictedTree().getRows(treeRequest("1"));

        // Bob's and Carol's children all fall below the threshold, so for this user they are leaves
        assertThat(columnValues(result, "isGroup")).containsExactly(false, false, false);
    }

    @Test
    void treeAggregationIgnoresRestrictedDescendants() {
        ServerSideGetRowsRequest request = treeRequest();
        request.getValueCols().add(valueCol("salary", "sum"));

        LoadSuccessParams result = restrictedTree().getRows(request);

        // group rows sum their descendants: Alice's visible ones are Bob + Carol + Grace
        assertThat(salaryFor(result, 1L)).isEqualTo(750.00);
        // Heidi's is Ivan alone
        assertThat(salaryFor(result, 8L)).isEqualTo(150.00);
    }

    // ---------------------------------------------------------------- set filter values

    @Test
    void setFilterValuesOfferOnlyVisibleValues() {
        // "Beta", "BETA", "Gamma" and "Epsilon" hold no visible trades, so they are not values a
        // restricted user should be offered in the set filter dropdown
        assertThat(restricted().supplySetFilterValues(FieldPath.of(Trade_.portfolio)))
                .containsExactlyInAnyOrder("Alpha", "alpha", "Delta", "delta");
    }

    // ---------------------------------------------------------------- helpers

    private static Map<String, Object> rowWhere(LoadSuccessParams result, String field, Object value) {
        return result.getRowData().stream()
                .filter(row -> value.equals(row.get(field)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no row with " + field + " = " + value));
    }

    private static double sumForBook(LoadSuccessParams result, String book) {
        return ((Number) rowWhere(result, "book", book).get("currentValue")).doubleValue();
    }

    private static List<Long> employeeIds(LoadSuccessParams result) {
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> row : result.getRowData()) {
            ids.add(((Number) row.get("employeeId")).longValue());
        }
        return ids;
    }

    private static long childCountFor(LoadSuccessParams result, long employeeId) {
        return ((Number) rowWhere(result, "employeeId", employeeId).get("childCount")).longValue();
    }

    private static double salaryFor(LoadSuccessParams result, long employeeId) {
        return ((Number) rowWhere(result, "employeeId", employeeId).get("salary")).doubleValue();
    }
}
