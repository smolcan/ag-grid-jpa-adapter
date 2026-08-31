package io.github.smolcan.aggrid.jpa.adapter.test.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Mapped superclass of {@link Invoice}, carrying the id, the audit columns, the product
 * association and the tree-data parent id. Its generated metamodel attributes are typed against
 * {@code AuditedRecord}, not against the entity, which is what the relaxed generics must accept.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class AuditedRecord {

    @Id
    private Long recordId;

    private String owner;

    @Column(precision = 20, scale = 2)
    private BigDecimal amount;

    private LocalDate createdOn;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "parent_record_id")
    private Long parentRecordId;
}
