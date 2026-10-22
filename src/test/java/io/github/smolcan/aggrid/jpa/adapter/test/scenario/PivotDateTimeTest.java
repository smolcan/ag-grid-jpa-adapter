package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortDirection;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.DealType;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pivoting on a date-time column with two values on the same day: trade 1 at 09:00 and an extra
 * trade at 15:30 on 2024-01-10.
 */
class PivotDateTimeTest extends ScenarioTestBase {

    @BeforeAll
    static void seedSecondTradeOnTheSameDay() {
        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();
        em.persist(new Trade(13L, "Alpha", "B-1", 113, new BigDecimal("1.00"), null, LocalDate.of(2024, 1, 10),
                LocalDateTime.of(2024, 1, 10, 15, 30, 0), null, true, DealType.BUY, null));
        em.getTransaction().commit();
        em.close();
    }

    @Test
    void sameDayDateTimesGetSeparatePivotColumns() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).build(),
                        ColDef.builder(Trade_.submittedAt).enablePivot(true).build()
                )
                .build();

        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setPivotMode(true);
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getPivotCols().add(groupCol("submittedAt"));
        request.getValueCols().add(valueCol("currentValue", "sum"));
        request.getSortModel().add(sortItem("portfolio", SortDirection.asc));

        LoadSuccessParams result = queryBuilder.getRows(request);

        // 12 distinct date-times and the null one of trade 8, nothing merged
        assertThat(result.getPivotResultFields())
                .hasSize(13)
                .contains("2024-01-10T09:00:00_currentValue", "2024-01-10T15:30:00_currentValue");

        Map<String, Object> alpha = result.getRowData().stream()
                .filter(row -> "Alpha".equals(row.get("portfolio")))
                .findFirst()
                .orElseThrow();
        assertThat(((Number) alpha.get("2024-01-10T09:00:00_currentValue")).doubleValue()).isEqualTo(100.00);
        assertThat(((Number) alpha.get("2024-01-10T15:30:00_currentValue")).doubleValue()).isEqualTo(1.00);
    }
}
