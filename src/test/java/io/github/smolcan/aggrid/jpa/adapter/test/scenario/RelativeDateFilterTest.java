package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.DateFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgDateColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The relative date options resolve against LocalDateTime.now() inside the adapter,
 * so this class seeds extra trades at offsets relative to today. Week/month/quarter
 * options are boundary- and locale-dependent and are left untested; the day-based
 * options below are deterministic for any run date. The standard dataset (2024/2025
 * dates) stays outside every tested window.
 */
class RelativeDateFilterTest extends ScenarioTestBase {

    private static final LocalDate TODAY = LocalDate.now();

    @BeforeAll
    static void seedRelativeTrades() {
        EntityManager em = entityManagerFactory.createEntityManager();
        em.getTransaction().begin();
        em.persist(relativeTrade(101L, TODAY));
        em.persist(relativeTrade(102L, TODAY.minusDays(1)));
        em.persist(relativeTrade(103L, TODAY.plusDays(1)));
        em.persist(relativeTrade(104L, TODAY.minusDays(10)));
        em.persist(relativeTrade(105L, TODAY.minusDays(100)));
        em.getTransaction().commit();
        em.close();
    }

    private static Trade relativeTrade(Long id, LocalDate tradeDate) {
        return new Trade(id, "Relative", null, null, BigDecimal.ONE, null, tradeDate, null, null, null, null, null);
    }

    private List<Long> rows(String relativeOption) {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.tradeDate)
                                .filter(AgDateColumnFilter.forLocalDate().filterParams(DateFilterParams.builder()
                                        .filterOptions(SimpleFilterModelType.values())
                                        .build()))
                                .build()
                )
                .build();

        ServerSideGetRowsRequest request = sortedByIdRequest(0, 100);
        request.setFilterModel(Map.of("tradeDate", filter(relativeOption)));
        return tradeIds(queryBuilder.getRows(request));
    }

    @Test
    void today() {
        assertThat(rows("today")).containsExactly(101L);
    }

    @Test
    void yesterday() {
        assertThat(rows("yesterday")).containsExactly(102L);
    }

    @Test
    void tomorrow() {
        assertThat(rows("tomorrow")).containsExactly(103L);
    }

    @Test
    void last7Days() {
        assertThat(rows("last7Days")).containsExactly(101L, 102L);
    }

    @Test
    void last30Days() {
        assertThat(rows("last30Days")).containsExactly(101L, 102L, 104L);
    }

    @Test
    void last90Days() {
        assertThat(rows("last90Days")).containsExactly(101L, 102L, 104L);
    }
}
