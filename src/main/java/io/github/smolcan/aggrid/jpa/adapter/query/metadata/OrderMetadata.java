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
    /**
     * @param order the JPA order (column + sort direction) to apply.
     * @return the JPA order to apply.
     */
    @NonNull
    private final Order order;
    /**
     * @param colId the ID of the ordered column.
     * @return the ID of the ordered column.
     */
    @NonNull
    private final String colId;
}
