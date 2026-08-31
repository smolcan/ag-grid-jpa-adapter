package io.github.smolcan.aggrid.jpa.adapter.test.entity;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity inheriting everything but {@code invoiceNumber} from {@link AuditedRecord}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Invoice extends AuditedRecord {

    private String invoiceNumber;

    public Invoice(Long recordId, String owner, BigDecimal amount, LocalDate createdOn, Product product, Long parentRecordId, String invoiceNumber) {
        this.setRecordId(recordId);
        this.setOwner(owner);
        this.setAmount(amount);
        this.setCreatedOn(createdOn);
        this.setProduct(product);
        this.setParentRecordId(parentRecordId);
        this.invoiceNumber = invoiceNumber;
    }
}
