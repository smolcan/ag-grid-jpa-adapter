package io.github.smolcan.aggrid.jpa.adapter.query.metadata;

import jakarta.persistence.criteria.Predicate;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Metadata for a {@code HAVING} clause predicate, including pivoting context.
 */
@Getter
@Builder
public class HavingMetadata {
    
    @NonNull
    private final Predicate predicate;
    private final boolean isPivoting;

}
