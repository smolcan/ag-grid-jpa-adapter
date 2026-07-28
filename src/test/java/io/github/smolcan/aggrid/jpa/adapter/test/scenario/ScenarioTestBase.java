package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgDateColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgNumberColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortDirection;
import io.github.smolcan.aggrid.jpa.adapter.request.SortModelItem;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.request.ColumnVO;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import io.github.smolcan.aggrid.jpa.adapter.test.infrastructure.EmployeeTestData;
import io.github.smolcan.aggrid.jpa.adapter.test.infrastructure.TestPersistence;
import io.github.smolcan.aggrid.jpa.adapter.test.infrastructure.TradeTestData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base for scenario tests: boots the persistence unit once per test class,
 * seeds {@link TradeTestData} and provides request/assertion helpers.
 */
public abstract class ScenarioTestBase {

    protected static EntityManagerFactory entityManagerFactory;
    protected EntityManager entityManager;

    @BeforeAll
    static void startPersistenceAndSeed() {
        entityManagerFactory = TestPersistence.createEntityManagerFactory();
        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();
        TradeTestData.seed(em);
        EmployeeTestData.seed(em);
        em.getTransaction().commit();
        em.close();
    }

    @AfterAll
    static void closePersistence() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @BeforeEach
    void openEntityManager() {
        entityManager = entityManagerFactory.createEntityManager();
    }

    @AfterEach
    void closeEntityManager() {
        if (entityManager != null) {
            entityManager.close();
        }
    }

    /**
     * QueryBuilder over all Trade columns with default filter params.
     * Tests needing custom params (case sensitivity, inclusive ranges, ...) build their own.
     */
    protected QueryBuilder<Trade, Long, Void> defaultQueryBuilder() {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Trade_.book).filter(new AgTextColumnFilter()).build(),
                        ColDef.builder(Trade_.submitterId).filter(new AgNumberColumnFilter<>()).build(),
                        ColDef.builder(Trade_.currentValue).filter(new AgNumberColumnFilter<>()).build(),
                        ColDef.builder(Trade_.previousValue).filter(new AgNumberColumnFilter<>()).build(),
                        ColDef.builder(Trade_.tradeDate).filter(AgDateColumnFilter.forLocalDate()).build(),
                        ColDef.builder(Trade_.submittedAt).filter(AgDateColumnFilter.forLocalDateTime()).build(),
                        ColDef.builder(FieldPath.of(Trade_.product).to(Product_.name)).filter(new AgTextColumnFilter()).build()
                )
                .build();
    }

    protected static ServerSideGetRowsRequest emptyRequest(int startRow, int endRow) {
        ServerSideGetRowsRequest request = new ServerSideGetRowsRequest();
        request.setStartRow(startRow);
        request.setEndRow(endRow);
        request.setRowGroupCols(new ArrayList<>());
        request.setValueCols(new ArrayList<>());
        request.setPivotCols(new ArrayList<>());
        request.setGroupKeys(new ArrayList<>());
        request.setSortModel(new ArrayList<>());
        return request;
    }

    /** Request sorted by tradeId so row order (and therefore assertions) are deterministic. */
    protected static ServerSideGetRowsRequest sortedByIdRequest(int startRow, int endRow) {
        ServerSideGetRowsRequest request = emptyRequest(startRow, endRow);
        request.getSortModel().add(sortItem("tradeId", SortDirection.asc));
        return request;
    }

    protected static SortModelItem sortItem(String colId, SortDirection direction) {
        SortModelItem item = new SortModelItem();
        item.setColId(colId);
        item.setSort(direction);
        return item;
    }

    protected static Map<String, Object> filter(String type) {
        Map<String, Object> model = new HashMap<>();
        model.put("type", type);
        return model;
    }

    protected static Map<String, Object> filter(String type, Object filterValue) {
        Map<String, Object> model = filter(type);
        model.put("filter", filterValue);
        return model;
    }

    protected static Map<String, Object> rangeFilter(Object from, Object to) {
        Map<String, Object> model = filter("inRange", from);
        model.put("filterTo", to);
        return model;
    }

    protected static Map<String, Object> dateFilter(String type, String dateFrom) {
        Map<String, Object> model = filter(type);
        model.put("dateFrom", dateFrom);
        return model;
    }

    protected static Map<String, Object> dateRangeFilter(String dateFrom, String dateTo) {
        Map<String, Object> model = dateFilter("inRange", dateFrom);
        model.put("dateTo", dateTo);
        return model;
    }

    @SafeVarargs
    protected static Map<String, Object> combined(String operator, Map<String, Object>... conditions) {
        Map<String, Object> model = new HashMap<>();
        model.put("operator", operator);
        model.put("conditions", List.of(conditions));
        return model;
    }

    protected static ColumnVO groupCol(String field) {
        ColumnVO col = new ColumnVO();
        col.setId(field);
        col.setField(field);
        col.setDisplayName(field);
        return col;
    }

    protected static ColumnVO valueCol(String field, String aggFunc) {
        ColumnVO col = groupCol(field);
        col.setAggFunc(aggFunc);
        return col;
    }

    protected static List<Long> tradeIds(LoadSuccessParams result) {
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> row : result.getRowData()) {
            ids.add(((Number) row.get("tradeId")).longValue());
        }
        return ids;
    }

    /** Extracts a value column per row as Double (aggregation results), null-safe. */
    protected static List<Double> doubleValues(LoadSuccessParams result, String field) {
        List<Double> values = new ArrayList<>();
        for (Map<String, Object> row : result.getRowData()) {
            Object value = row.get(field);
            values.add(value == null ? null : ((Number) value).doubleValue());
        }
        return values;
    }

    protected static List<Object> columnValues(LoadSuccessParams result, String field) {
        List<Object> values = new ArrayList<>();
        for (Map<String, Object> row : result.getRowData()) {
            values.add(row.get(field));
        }
        return values;
    }

    /** Navigates the nested maps produced for dot-notation fields ("product.name" -> {product={name=...}}). */
    @SuppressWarnings("unchecked")
    protected static Object nestedValue(Map<String, Object> row, String dotPath) {
        String[] parts = dotPath.split("\\.");
        Object current = row;
        for (String part : parts) {
            if (current == null) {
                return null;
            }
            current = ((Map<String, Object>) current).get(part);
        }
        return current;
    }
}
