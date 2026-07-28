package io.github.smolcan.aggrid.jpa.adapter.test.infrastructure;

import io.github.smolcan.aggrid.jpa.adapter.test.entity.DealType;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Deterministic dataset shared by all scenario tests. Notable rows:
 * <ul>
 *   <li>portfolio has case variants (Alpha/alpha, Beta/BETA, delta/Delta)</li>
 *   <li>book is null on 3 and 8, empty string on 4</li>
 *   <li>submitterId is null on 5</li>
 *   <li>previousValue is null on 2, 6, 12</li>
 *   <li>tradeDate and submittedAt are null on 8</li>
 *   <li>product, externalId are null on 10; sold is null on 8 and 12</li>
 *   <li>currentValue 100.00 appears twice (1, 11), negative on 3 and 7</li>
 * </ul>
 */
public final class TradeTestData {

    public static final int TRADE_COUNT = 12;

    private TradeTestData() {
    }

    public static UUID externalId(long tradeId) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", tradeId));
    }

    public static void seed(EntityManager em) {
        Product gold = new Product(1L, "Gold");
        Product silver = new Product(2L, "Silver");
        Product platinum = new Product(3L, "Platinum");
        em.persist(gold);
        em.persist(silver);
        em.persist(platinum);

        em.persist(new Trade(1L, "Alpha", "B-1", 101, new BigDecimal("100.00"), 90.0, LocalDate.of(2024, 1, 10), LocalDateTime.of(2024, 1, 10, 9, 0, 0), gold, true, DealType.BUY, externalId(1)));
        em.persist(new Trade(2L, "Alpha", "B-2", 102, new BigDecimal("250.50"), null, LocalDate.of(2024, 2, 15), LocalDateTime.of(2024, 2, 15, 10, 30, 0), silver, true, DealType.SELL, externalId(2)));
        em.persist(new Trade(3L, "alpha", null, 103, new BigDecimal("-75.25"), 60.0, LocalDate.of(2024, 3, 20), LocalDateTime.of(2024, 3, 20, 11, 0, 0), gold, true, DealType.BUY, externalId(3)));
        em.persist(new Trade(4L, "Beta", "", 104, new BigDecimal("0.00"), 10.5, LocalDate.of(2024, 4, 1), LocalDateTime.of(2024, 4, 1, 12, 0, 0), platinum, false, DealType.HOLD, externalId(4)));
        em.persist(new Trade(5L, "Beta", "B-3", null, new BigDecimal("500.00"), 450.0, LocalDate.of(2024, 5, 5), LocalDateTime.of(2024, 5, 5, 13, 45, 0), silver, false, DealType.SELL, externalId(5)));
        em.persist(new Trade(6L, "BETA", "B-1", 106, new BigDecimal("320.10"), null, LocalDate.of(2024, 6, 30), LocalDateTime.of(2024, 6, 30, 14, 0, 0), gold, false, DealType.BUY, externalId(6)));
        em.persist(new Trade(7L, "Gamma", "B-4", 107, new BigDecimal("-10.00"), -5.0, LocalDate.of(2024, 7, 4), LocalDateTime.of(2024, 7, 4, 15, 15, 0), platinum, false, DealType.HOLD, externalId(7)));
        em.persist(new Trade(8L, "Gamma", null, 108, new BigDecimal("75.25"), 70.0, null, null, silver, null, DealType.SELL, externalId(8)));
        em.persist(new Trade(9L, "delta", "B-5", 109, new BigDecimal("150.00"), 140.0, LocalDate.of(2025, 1, 1), LocalDateTime.of(2025, 1, 1, 8, 0, 0), gold, true, DealType.BUY, externalId(9)));
        em.persist(new Trade(10L, "Delta", "B-2", 110, new BigDecimal("999.99"), 1000.0, LocalDate.of(2025, 2, 14), LocalDateTime.of(2025, 2, 14, 9, 30, 0), null, true, DealType.SELL, null));
        em.persist(new Trade(11L, "Epsilon", "B-6", 111, new BigDecimal("100.00"), 95.5, LocalDate.of(2025, 3, 3), LocalDateTime.of(2025, 3, 3, 10, 0, 0), platinum, false, DealType.HOLD, externalId(11)));
        em.persist(new Trade(12L, "Epsilon", "b-1", 112, new BigDecimal("42.42"), null, LocalDate.of(2025, 4, 18), LocalDateTime.of(2025, 4, 18, 11, 11, 11), silver, null, DealType.BUY, externalId(12)));
    }
}
