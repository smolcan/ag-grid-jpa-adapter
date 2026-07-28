package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgNumberColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortDirection;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Employee;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Employee_;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TreeDataTest extends ScenarioTestBase {

    private enum ParentMode { REFERENCE_FIELD, ID_FIELD, CHILDREN_FIELD }

    private QueryBuilder<Employee, Long, Void> treeQueryBuilder(ParentMode mode, boolean withChildCount) {
        return treeQueryBuilderConfig(mode, withChildCount).build();
    }

    /** Unbuilt tree config, so tests can layer extra filtering options on top. */
    private QueryBuilder.Builder<Employee, Long, Void> treeQueryBuilderConfig(ParentMode mode, boolean withChildCount) {
        QueryBuilder.Builder<Employee, Long, Void> builder = QueryBuilder.builder(Employee.class, Employee_.employeeId, entityManager)
                .colDefs(
                        ColDef.builder(Employee_.employeeId).build(),
                        ColDef.builder(Employee_.name).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Employee_.salary).enableValue(true).filter(new AgNumberColumnFilter<>()).build()
                )
                .treeData(true)
                .isServerSideGroupFieldName("isGroup")
                .treeDataStringToParentIdTypeConverter(Long::valueOf)
                .treeDataDataPathFieldName(Employee_.path)
                .treeDataDataPathSeparator("/");

        if (mode == ParentMode.ID_FIELD) {
            builder.treeDataParentIdField(Employee_.managerId);
        } else {
            builder.treeDataParentReferenceField(Employee_.manager);
        }
        if (mode == ParentMode.CHILDREN_FIELD) {
            builder.treeDataChildrenField(Employee_.children);
        }
        if (withChildCount) {
            builder.getChildCount(true).getChildCountFieldName("childCount");
        }
        return builder;
    }

    private ServerSideGetRowsRequest treeRequest(String... groupKeys) {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        for (String key : groupKeys) {
            request.getGroupKeys().add(key);
        }
        request.getSortModel().add(sortItem("employeeId", SortDirection.asc));
        return request;
    }

    private static List<Long> employeeIds(LoadSuccessParams result) {
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> row : result.getRowData()) {
            ids.add(((Number) row.get("employeeId")).longValue());
        }
        return ids;
    }

    @Test
    void rootLevelReturnsRootsWithGroupFlags() {
        LoadSuccessParams result = treeQueryBuilder(ParentMode.REFERENCE_FIELD, false).getRows(treeRequest());
        assertThat(employeeIds(result)).containsExactly(1L, 8L, 10L);
        assertThat(columnValues(result, "isGroup")).containsExactly(true, true, false);
    }

    @Test
    void drillDownReturnsDirectChildren() {
        LoadSuccessParams result = treeQueryBuilder(ParentMode.REFERENCE_FIELD, false).getRows(treeRequest("1"));
        assertThat(employeeIds(result)).containsExactly(2L, 3L, 7L);
        assertThat(columnValues(result, "isGroup")).containsExactly(true, true, false);
    }

    @Test
    void deepDrillDownReturnsLeaves() {
        LoadSuccessParams result = treeQueryBuilder(ParentMode.REFERENCE_FIELD, false).getRows(treeRequest("1", "2"));
        assertThat(employeeIds(result)).containsExactly(4L, 5L);
        assertThat(columnValues(result, "isGroup")).containsExactly(false, false);
    }

    @Test
    void parentIdFieldModeBehavesTheSame() {
        LoadSuccessParams roots = treeQueryBuilder(ParentMode.ID_FIELD, false).getRows(treeRequest());
        assertThat(employeeIds(roots)).containsExactly(1L, 8L, 10L);

        LoadSuccessParams children = treeQueryBuilder(ParentMode.ID_FIELD, false).getRows(treeRequest("8"));
        assertThat(employeeIds(children)).containsExactly(9L);
    }

    @Test
    void childrenFieldModeComputesGroupFlagFromCollection() {
        LoadSuccessParams result = treeQueryBuilder(ParentMode.CHILDREN_FIELD, false).getRows(treeRequest());
        assertThat(employeeIds(result)).containsExactly(1L, 8L, 10L);
        assertThat(columnValues(result, "isGroup")).containsExactly(true, true, false);
    }

    @Test
    void filterKeepsAncestorsOfMatchingDescendants() {
        ServerSideGetRowsRequest request = treeRequest();
        request.setFilterModel(Map.of("salary", filter("lessThan", 100)));

        LoadSuccessParams result = treeQueryBuilder(ParentMode.REFERENCE_FIELD, false).getRows(request);
        // only Frank (90, path 1/3/6) matches; root 1 stays because a descendant matches
        assertThat(employeeIds(result)).containsExactly(1L);
    }

    @Test
    void filterKeepsOwnMatchesAtRootLevel() {
        ServerSideGetRowsRequest request = treeRequest();
        request.setFilterModel(Map.of("salary", filter("greaterThan", 350)));

        LoadSuccessParams result = treeQueryBuilder(ParentMode.REFERENCE_FIELD, false).getRows(request);
        // Alice (500) and Heidi (400) match themselves; Judy (130) has no children -> dropped
        assertThat(employeeIds(result)).containsExactly(1L, 8L);
    }

    @Test
    void filterKeepsAllChildrenWhenExpandedParentMatches() {
        ServerSideGetRowsRequest request = treeRequest("1");
        request.setFilterModel(Map.of("salary", filter("greaterThan", 450)));

        LoadSuccessParams result = treeQueryBuilder(ParentMode.REFERENCE_FIELD, false).getRows(request);
        // only Alice (500) matches, but as the expanded parent she exposes all her children
        assertThat(employeeIds(result)).containsExactly(2L, 3L, 7L);
    }

    @Test
    void aggregationSumsDescendantsForGroupsAndOwnValueForLeaves() {
        ServerSideGetRowsRequest request = treeRequest();
        request.getValueCols().add(valueCol("salary", "sum"));

        LoadSuccessParams result = treeQueryBuilder(ParentMode.REFERENCE_FIELD, false).getRows(request);
        // Alice: sum of all 6 descendants (300+250+100+120+90+200); Heidi: Ivan only; Judy: own value
        assertThat(doubleValues(result, "salary")).containsExactly(1060.00, 150.00, 130.00);
    }

    @Test
    void childCountCountsAllDescendants() {
        LoadSuccessParams result = treeQueryBuilder(ParentMode.REFERENCE_FIELD, true).getRows(treeRequest());
        assertThat(doubleValues(result, "childCount")).containsExactly(6.0, 1.0, 0.0);
    }

    @Test
    void childCountRespectsFilters() {
        ServerSideGetRowsRequest request = treeRequest();
        request.setFilterModel(Map.of("salary", filter("greaterThan", 100)));

        LoadSuccessParams result = treeQueryBuilder(ParentMode.REFERENCE_FIELD, true).getRows(request);
        assertThat(employeeIds(result)).containsExactly(1L, 8L, 10L);
        // only descendants passing the filter are counted: Alice keeps Bob/Carol/Eve/Grace (Dave=100 and Frank=90 fail)
        assertThat(doubleValues(result, "childCount")).containsExactly(4.0, 1.0, 0.0);
    }

    @Test
    void childCountRespectsExternalFilter() {
        QueryBuilder<Employee, Long, Void> queryBuilder = treeQueryBuilderConfig(ParentMode.REFERENCE_FIELD, true)
                .isExternalFilterPresent(true)
                .doesExternalFilterPass((cb, root, externalFilterValue) ->
                        cb.gt(root.get(Employee_.salary), new BigDecimal(externalFilterValue.toString())))
                .build();

        ServerSideGetRowsRequest request = treeRequest();
        request.setExternalFilter("100");

        LoadSuccessParams result = queryBuilder.getRows(request);
        assertThat(employeeIds(result)).containsExactly(1L, 8L, 10L);
        // the external filter reaches the child-count subquery, same descendants as childCountRespectsFilters
        assertThat(doubleValues(result, "childCount")).containsExactly(4.0, 1.0, 0.0);
    }

    @Test
    void childCountRespectsQuickFilter() {
        QueryBuilder<Employee, Long, Void> queryBuilder = treeQueryBuilderConfig(ParentMode.REFERENCE_FIELD, true)
                .isQuickFilterPresent(true)
                .quickFilterSearchInFields(FieldPath.of(Employee_.name))
                .build();

        ServerSideGetRowsRequest request = treeRequest();
        request.setQuickFilter("o");

        LoadSuccessParams result = queryBuilder.getRows(request);
        // only Bob and Carol contain "o"; Alice stays as their ancestor and counts exactly those two
        assertThat(employeeIds(result)).containsExactly(1L);
        assertThat(doubleValues(result, "childCount")).containsExactly(2.0);
    }

    @Test
    void childCountRespectsAdvancedFilter() {
        QueryBuilder<Employee, Long, Void> queryBuilder = treeQueryBuilderConfig(ParentMode.REFERENCE_FIELD, true)
                .enableAdvancedFilter(true)
                .build();

        ServerSideGetRowsRequest request = treeRequest();
        request.setFilterModel(Map.of("filterType", "number", "colId", "salary", "type", "greaterThan", "filter", 100));

        LoadSuccessParams result = queryBuilder.getRows(request);
        assertThat(employeeIds(result)).containsExactly(1L, 8L, 10L);
        assertThat(doubleValues(result, "childCount")).containsExactly(4.0, 1.0, 0.0);
    }

    @Test
    void aggregationRespectsFilters() {
        ServerSideGetRowsRequest request = treeRequest();
        request.setFilterModel(Map.of("salary", filter("greaterThan", 100)));
        request.getValueCols().add(valueCol("salary", "sum"));

        LoadSuccessParams result = treeQueryBuilder(ParentMode.REFERENCE_FIELD, false).getRows(request);
        assertThat(employeeIds(result)).containsExactly(1L, 8L, 10L);
        // Alice: 300+250+120+200 (Dave and Frank filtered out); Heidi: Ivan; Judy: own value
        assertThat(doubleValues(result, "salary")).containsExactly(870.00, 150.00, 130.00);
    }

    /**
     * Drill-down where only the expanded parent matches: her children are returned solely through the
     * parent-match subquery, and their aggregates cover only matching descendants (here: none).
     * The three tests below run that same scenario through each filtering mechanism.
     */
    @Test
    void externalFilterAppliesToParentMatchAndAggregation() {
        QueryBuilder<Employee, Long, Void> queryBuilder = treeQueryBuilderConfig(ParentMode.REFERENCE_FIELD, false)
                .isExternalFilterPresent(true)
                .doesExternalFilterPass((cb, root, externalFilterValue) ->
                        cb.gt(root.get(Employee_.salary), new BigDecimal(externalFilterValue.toString())))
                .build();

        ServerSideGetRowsRequest request = treeRequest("1");
        request.setExternalFilter("450");
        request.getValueCols().add(valueCol("salary", "sum"));

        LoadSuccessParams result = queryBuilder.getRows(request);
        assertThat(employeeIds(result)).containsExactly(2L, 3L, 7L);
        // Bob and Carol aggregate nothing (no descendant above 450), Grace is a leaf and keeps her own value
        assertThat(doubleValues(result, "salary")).containsExactly(null, null, 200.00);
    }

    @Test
    void quickFilterAppliesToParentMatchAndAggregation() {
        QueryBuilder<Employee, Long, Void> queryBuilder = treeQueryBuilderConfig(ParentMode.REFERENCE_FIELD, false)
                .isQuickFilterPresent(true)
                .quickFilterSearchInFields(FieldPath.of(Employee_.name))
                .build();

        ServerSideGetRowsRequest request = treeRequest("1");
        request.setQuickFilter("ali");
        request.getValueCols().add(valueCol("salary", "sum"));

        LoadSuccessParams result = queryBuilder.getRows(request);
        assertThat(employeeIds(result)).containsExactly(2L, 3L, 7L);
        assertThat(doubleValues(result, "salary")).containsExactly(null, null, 200.00);
    }

    @Test
    void advancedFilterAppliesToParentMatchAndAggregation() {
        QueryBuilder<Employee, Long, Void> queryBuilder = treeQueryBuilderConfig(ParentMode.REFERENCE_FIELD, false)
                .enableAdvancedFilter(true)
                .build();

        ServerSideGetRowsRequest request = treeRequest("1");
        request.setFilterModel(Map.of("filterType", "number", "colId", "salary", "type", "greaterThan", "filter", 450));
        request.getValueCols().add(valueCol("salary", "sum"));

        LoadSuccessParams result = queryBuilder.getRows(request);
        assertThat(employeeIds(result)).containsExactly(2L, 3L, 7L);
        assertThat(doubleValues(result, "salary")).containsExactly(null, null, 200.00);
    }

    @Test
    void countRowsCountsCurrentTreeLevel() {
        long count = treeQueryBuilder(ParentMode.REFERENCE_FIELD, false).countRows(treeRequest());
        assertThat(count).isEqualTo(3);
    }
}
