package io.github.smolcan.aggrid.jpa.adapter.query.metadata;

import jakarta.persistence.criteria.Predicate;

import java.util.Objects;

/**
 * Metadata for a {@code HAVING} clause predicate, including pivoting context.
 */
public class HavingMetadata {
    
    private final Predicate predicate;
    private final boolean isPivoting;

    private HavingMetadata(Builder builder) {
        this.predicate = builder.predicate;
        this.isPivoting = builder.isPivoting;
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public boolean isPivoting() {
        return isPivoting;
    }

    public static Builder builder(Predicate predicate) {
        return new Builder(predicate);
    }

    public static class Builder {
        private Predicate predicate;
        private boolean isPivoting;
        
        public Builder(Predicate predicate) {
            Objects.requireNonNull(predicate);
            this.predicate = predicate;
        }

        public Builder predicate(Predicate predicate) {
            Objects.requireNonNull(predicate);
            this.predicate = predicate;
            return this;
        }

        public Builder isPivoting(boolean isPivoting) {
            this.isPivoting = isPivoting;
            return this;
        }

        public HavingMetadata build() {
            return new HavingMetadata(this);
        }
    }
}
