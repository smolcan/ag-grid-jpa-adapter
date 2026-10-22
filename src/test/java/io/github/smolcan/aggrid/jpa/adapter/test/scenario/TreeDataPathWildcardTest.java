package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgNumberColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortDirection;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Employee;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Employee_;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tree data whose paths contain LIKE wildcards. Each wildcard root has a lookalike root that a
 * {@code LIKE} pattern built from its path would also match: "a_b/%" matches "axb/104" and
 * "c%/%" matches "cd/108", so the descendants of the lookalike would count as its own.
 */
class TreeDataPathWildcardTest extends ScenarioTestBase {

    @BeforeAll
    static void seedWildcardPaths() {
        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();

        Employee underscore = new Employee(101L, "Underscore", new BigDecimal("10.00"), null, "a_b");
        Employee lookalike = new Employee(103L, "Lookalike", new BigDecimal("30.00"), null, "axb");
        Employee percent = new Employee(105L, "Percent", new BigDecimal("50.00"), null, "c%");
        Employee similar = new Employee(107L, "Similar", new BigDecimal("70.00"), null, "cd");
        em.persist(underscore);
        em.persist(new Employee(102L, "Underscore child", new BigDecimal("20.00"), underscore, "a_b/102"));
        em.persist(lookalike);
        em.persist(new Employee(104L, "Lookalike child", new BigDecimal("40.00"), lookalike, "axb/104"));
        em.persist(percent);
        em.persist(new Employee(106L, "Percent child", new BigDecimal("60.00"), percent, "c%/106"));
        em.persist(similar);
        em.persist(new Employee(108L, "Similar child", new BigDecimal("80.00"), similar, "cd/108"));

        em.getTransaction().commit();
        em.close();
    }

    private QueryBuilder<Employee, Long, Void> treeQueryBuilder() {
        return QueryBuilder.builder(Employee.class, Employee_.employeeId, entityManager)
                .colDefs(
                        ColDef.builder(Employee_.employeeId).build(),
                        ColDef.builder(Employee_.name).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Employee_.salary).enableValue(true).filter(new AgNumberColumnFilter<>()).build()
                )
                .treeData(true)
                .isServerSideGroupFieldName("isGroup")
                .treeDataStringToParentIdTypeConverter(Long::valueOf)
                .treeDataParentReferenceField(Employee_.manager)
                .treeDataDataPathFieldName(Employee_.path)
                .treeDataDataPathSeparator("/")
                .getChildCount(true)
                .getChildCountFieldName("childCount")
                .build();
    }

    private static ServerSideGetRowsRequest rootRequest() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
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
    void childCountDoesNotIncludeLookalikeDescendants() {
        LoadSuccessParams result = treeQueryBuilder().getRows(rootRequest());

        // roots 1, 8, 10 from the shared fixture, then a_b, axb, c%, cd with one child each
        assertThat(employeeIds(result)).containsExactly(1L, 8L, 10L, 101L, 103L, 105L, 107L);
        assertThat(doubleValues(result, "childCount")).containsExactly(6.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0);
    }

    @Test
    void aggregationDoesNotIncludeLookalikeDescendants() {
        ServerSideGetRowsRequest request = rootRequest();
        request.getValueCols().add(valueCol("salary", "sum"));

        LoadSuccessParams result = treeQueryBuilder().getRows(request);

        // a_b sums only its own child (20), not axb's (40); c% only its own (60), not cd's (80)
        assertThat(doubleValues(result, "salary")).containsExactly(1060.0, 150.0, 130.0, 20.0, 40.0, 60.0, 80.0);
    }

    @Test
    void filterDoesNotKeepARootForALookalikeDescendant() {
        ServerSideGetRowsRequest request = rootRequest();
        request.setFilterModel(Map.of("name", filter("equals", "Lookalike child")));
        assertThat(employeeIds(treeQueryBuilder().getRows(request))).containsExactly(103L);

        request.setFilterModel(Map.of("name", filter("equals", "Similar child")));
        assertThat(employeeIds(treeQueryBuilder().getRows(request))).containsExactly(107L);
    }
}
