package io.github.smolcan.aggrid.jpa.adapter.test.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Trade {

    @Id
    private Long tradeId;

    private String portfolio;

    private String book;

    private Integer submitterId;

    @Column(precision = 20, scale = 2)
    private BigDecimal currentValue;

    private Double previousValue;

    private LocalDate tradeDate;

    private LocalDateTime submittedAt;

    @ManyToOne
    private Product product;

    private Boolean sold;

    @Enumerated(EnumType.STRING)
    private DealType dealType;

    private UUID externalId;

    /** Read-only mapping of the product FK column, for detailMasterIdField-style tests. */
    @Column(name = "product_productId", insertable = false, updatable = false)
    private Long productId;

    public Trade(Long tradeId, String portfolio, String book, Integer submitterId,
                 BigDecimal currentValue, Double previousValue, LocalDate tradeDate, LocalDateTime submittedAt,
                 Product product, Boolean sold, DealType dealType, UUID externalId) {
        this.tradeId = tradeId;
        this.portfolio = portfolio;
        this.book = book;
        this.submitterId = submitterId;
        this.currentValue = currentValue;
        this.previousValue = previousValue;
        this.tradeDate = tradeDate;
        this.submittedAt = submittedAt;
        this.product = product;
        this.sold = sold;
        this.dealType = dealType;
        this.externalId = externalId;
    }
}
