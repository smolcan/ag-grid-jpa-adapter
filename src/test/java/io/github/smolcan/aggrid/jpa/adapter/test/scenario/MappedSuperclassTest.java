package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.column.ComputedField;
import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgDateColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgNumberColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.AggregationFunction;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortDirection;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.AuditedRecord;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.AuditedRecord_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Invoice;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Invoice_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product_;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invoice inherits everything but {@code invoiceNumber} from the {@code AuditedRecord} mapped
 * superclass, so those metamodel attributes are {@code SingularAttribute<AuditedRecord, ...>}
 * while the grid queries {@code Invoice}. Every column below is fed from {@code AuditedRecord_}
 * to cover the relaxed generics: primary field, plain columns, a nested path through a
 * superclass-declared association, a computed field declared against the superclass, the quick
 * filter fields and the tree-data parent id.
 */
class MappedSuperclassTest extends ScenarioTestBase {

    @BeforeAll
    static void seedInvoices() {
        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();
        Product gold = em.find(Product.class, 1L);
        Product silver = em.find(Product.class, 2L);
        Product platinum = em.find(Product.class, 3L);
        em.persist(new Invoice(1L, "Acme", new BigDecimal("100.00"), LocalDate.of(2024, 1, 5), gold, null, "INV-1"));
        em.persist(new Invoice(2L, "Acme", new BigDecimal("250.50"), LocalDate.of(2024, 2, 10), silver, 1L, "INV-2"));
        em.persist(new Invoice(3L, "Globex", new BigDecimal("75.25"), LocalDate.of(2024, 3, 15), gold, 1L, "INV-3"));
        em.persist(new Invoice(4L, "Globex", new BigDecimal("500.00"), LocalDate.of(2024, 4, 20), platinum, null, "INV-4"));
        em.persist(new Invoice(5L, "Initech", new BigDecimal("42.42"), LocalDate.of(2024, 5, 25), null, 4L, "INV-5"));
        em.getTransaction().commit();
        em.close();
    }

    /** Declared against the mapped superclass, not against Invoice. */
    private static ComputedField<AuditedRecord, String> ownerUpper() {
        return ComputedField.<AuditedRecord, String>builder()
                .name("ownerUpper")
                .javaType(String.class)
                .expressionFunction((cb, root) -> cb.upper(root.get(AuditedRecord_.owner)))
                .build();
    }

    private QueryBuilder.Builder<Invoice, Long, Void> config() {
        return QueryBuilder.builder(Invoice.class, AuditedRecord_.recordId, entityManager)
                .colDefs(
                        ColDef.builder(AuditedRecord_.recordId).filter(new AgNumberColumnFilter<>()).build(),
                        ColDef.builder(AuditedRecord_.owner).filter(new AgTextColumnFilter()).enableRowGroup(true, key -> key).build(),
                        ColDef.builder(AuditedRecord_.amount).filter(new AgNumberColumnFilter<>()).enableValue(true).allowedAggFuncs(AggregationFunction.sum).build(),
                        ColDef.builder(AuditedRecord_.createdOn).filter(AgDateColumnFilter.forLocalDate()).build(),
                        ColDef.builder(Invoice_.invoiceNumber).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(FieldPath.of(AuditedRecord_.product).to(Product_.name)).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(ownerUpper()).filter(new AgTextColumnFilter()).build()
                );
    }

    private QueryBuilder<Invoice, Long, Void> invoiceQueryBuilder() {
        return config().build();
    }

    private static ServerSideGetRowsRequest sortedByRecordIdRequest() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getSortModel().add(sortItem("recordId", SortDirection.asc));
        return request;
    }

    private static List<Long> recordIds(LoadSuccessParams result) {
        return result.getRowData().stream()
                .map(row -> ((Number) row.get("recordId")).longValue())
                .collect(Collectors.toList());
    }

    @Test
    void selectsColumnsInheritedFromTheMappedSuperclass() {
        LoadSuccessParams result = invoiceQueryBuilder().getRows(sortedByRecordIdRequest());

        assertThat(recordIds(result)).containsExactly(1L, 2L, 3L, 4L, 5L);
        Map<String, Object> first = result.getRowData().get(0);
        assertThat(first.get("owner")).isEqualTo("Acme");
        assertThat(first.get("invoiceNumber")).isEqualTo("INV-1");
        assertThat(first.get("ownerUpper")).isEqualTo("ACME");
        assertThat(nestedValue(first, "product.name")).isEqualTo("Gold");
    }

    @Test
    void filtersOnAColumnInheritedFromTheMappedSuperclass() {
        ServerSideGetRowsRequest request = sortedByRecordIdRequest();
        Map<String, Object> filterModel = new HashMap<>();
        filterModel.put("owner", filter("equals", "Globex"));
        request.setFilterModel(filterModel);

        assertThat(recordIds(invoiceQueryBuilder().getRows(request))).containsExactly(3L, 4L);
    }

    @Test
    void filtersOnANestedPathThroughASuperclassDeclaredAssociation() {
        ServerSideGetRowsRequest request = sortedByRecordIdRequest();
        Map<String, Object> filterModel = new HashMap<>();
        filterModel.put("product.name", filter("equals", "Gold"));
        request.setFilterModel(filterModel);

        assertThat(recordIds(invoiceQueryBuilder().getRows(request))).containsExactly(1L, 3L);
    }

    @Test
    void sortsOnAColumnInheritedFromTheMappedSuperclass() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getSortModel().add(sortItem("amount", SortDirection.desc));

        assertThat(recordIds(invoiceQueryBuilder().getRows(request))).containsExactly(4L, 2L, 1L, 3L, 5L);
    }

    @Test
    void groupsAndAggregatesOnColumnsInheritedFromTheMappedSuperclass() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setFilterModel(new HashMap<>());
        request.getRowGroupCols().add(groupCol("owner"));
        request.getValueCols().add(valueCol("amount", "sum"));
        request.getSortModel().add(sortItem("owner", SortDirection.asc));

        LoadSuccessParams result = invoiceQueryBuilder().getRows(request);

        assertThat(columnValues(result, "owner")).containsExactly("Acme", "Globex", "Initech");
        assertThat(doubleValues(result, "amount")).containsExactly(350.50, 575.25, 42.42);
    }

    @Test
    void quickFiltersOnAFieldInheritedFromTheMappedSuperclass() {
        QueryBuilder<Invoice, Long, Void> queryBuilder = config()
                .isQuickFilterPresent(true)
                .quickFilterSearchInFields(FieldPath.of(AuditedRecord_.owner))
                .build();

        ServerSideGetRowsRequest request = sortedByRecordIdRequest();
        request.setQuickFilter("Acme");

        assertThat(recordIds(queryBuilder.getRows(request))).containsExactly(1L, 2L);
    }

    @Test
    void suppliesSetFilterValuesForAFieldInheritedFromTheMappedSuperclass() {
        List<String> owners = invoiceQueryBuilder().supplySetFilterValues(FieldPath.of(AuditedRecord_.owner));

        assertThat(owners).containsExactly("Acme", "Globex", "Initech");
    }

    @Test
    void treeDataUsesAParentIdInheritedFromTheMappedSuperclass() {
        QueryBuilder<Invoice, Long, Void> queryBuilder = config()
                .treeData(true)
                .isServerSideGroupFieldName("isGroup")
                .treeDataParentIdField(AuditedRecord_.parentRecordId)
                .treeDataStringToParentIdTypeConverter(Long::valueOf)
                .build();

        // top level: the two invoices without a parent
        ServerSideGetRowsRequest request = sortedByRecordIdRequest();
        assertThat(recordIds(queryBuilder.getRows(request))).containsExactly(1L, 4L);

        // children of invoice 1
        ServerSideGetRowsRequest childRequest = sortedByRecordIdRequest();
        childRequest.getGroupKeys().add("1");
        assertThat(recordIds(queryBuilder.getRows(childRequest))).containsExactly(2L, 3L);
    }
}
