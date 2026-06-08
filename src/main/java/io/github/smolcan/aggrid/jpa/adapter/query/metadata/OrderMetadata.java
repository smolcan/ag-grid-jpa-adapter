package io.github.smolcan.aggrid.jpa.adapter.query.metadata;

import jakarta.persistence.criteria.Order;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Metadata for a query {@link Order}, including the associated column ID.
 */
@Getter
@Builder
public class OrderMetadata {
    @NonNull
    private final Order order;
    @NonNull
    private final String colId;
}
