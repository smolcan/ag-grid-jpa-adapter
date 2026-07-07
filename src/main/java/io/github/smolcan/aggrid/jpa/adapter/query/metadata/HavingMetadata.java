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
    
    /**
     * @param predicate the {@code HAVING} predicate.
     * @return the {@code HAVING} predicate.
     */
    @NonNull
    private final Predicate predicate;
    /**
     * @param isPivoting whether this predicate belongs to a pivoting query.
     * @return whether this predicate belongs to a pivoting query.
     */
    private final boolean isPivoting;

}
