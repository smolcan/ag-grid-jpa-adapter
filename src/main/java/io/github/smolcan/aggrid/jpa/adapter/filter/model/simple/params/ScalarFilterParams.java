package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

public class ScalarFilterParams implements ISimpleFilterParams {
    // If true, the 'inRange' filter option will include values equal to the start and end of the range.
    private final boolean inRangeInclusive;
    // If true, blank (null or undefined) values will pass the 'equals' filter option.
    private final boolean includeBlanksInEquals;
    // If true, blank (null or undefined) values will pass the 'notEqual' filter option.
    private final boolean includeBlanksInNotEqual;
    // If `true`, blank (`null` or `undefined`) values will pass the `'lessThan'` and `'lessThanOrEqual'` filter options.
    private final boolean includeBlanksInLessThan;
    // If `true`, blank (`null` or `undefined`) values will pass the `'greaterThan'` and `'greaterThanOrEqual'` filter options.
    private final boolean includeBlanksInGreaterThan;
    // If `true`, blank (`null` or `undefined`) values will pass the `'inRange'` filter option.
    private final boolean includeBlanksInRange;

    public ScalarFilterParams(Builder builder) {
        this.inRangeInclusive = builder.inRangeInclusive;
        this.includeBlanksInEquals = builder.includeBlanksInEquals;
        this.includeBlanksInNotEqual = builder.includeBlanksInNotEqual;
        this.includeBlanksInLessThan = builder.includeBlanksInLessThan;
        this.includeBlanksInGreaterThan = builder.includeBlanksInGreaterThan;
        this.includeBlanksInRange = builder.includeBlanksInRange;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public boolean isInRangeInclusive() {
        return inRangeInclusive;
    }

    public boolean isIncludeBlanksInEquals() {
        return includeBlanksInEquals;
    }

    public boolean isIncludeBlanksInNotEqual() {
        return includeBlanksInNotEqual;
    }

    public boolean isIncludeBlanksInLessThan() {
        return includeBlanksInLessThan;
    }

    public boolean isIncludeBlanksInGreaterThan() {
        return includeBlanksInGreaterThan;
    }

    public boolean isIncludeBlanksInRange() {
        return includeBlanksInRange;
    }

    public static class Builder {
        protected boolean inRangeInclusive;
        protected boolean includeBlanksInEquals;
        protected boolean includeBlanksInNotEqual;
        protected boolean includeBlanksInLessThan;
        protected boolean includeBlanksInGreaterThan;
        protected boolean includeBlanksInRange;

        public Builder inRangeInclusive(boolean inRangeInclusive) {
            this.inRangeInclusive = inRangeInclusive;
            return this;
        }

        public Builder includeBlanksInEquals(boolean includeBlanksInEquals) {
            this.includeBlanksInEquals = includeBlanksInEquals;
            return this;
        }

        public Builder includeBlanksInNotEqual(boolean includeBlanksInNotEqual) {
            this.includeBlanksInNotEqual = includeBlanksInNotEqual;
            return this;
        }

        public Builder includeBlanksInLessThan(boolean includeBlanksInLessThan) {
            this.includeBlanksInLessThan = includeBlanksInLessThan;
            return this;
        }

        public Builder includeBlanksInGreaterThan(boolean includeBlanksInGreaterThan) {
            this.includeBlanksInGreaterThan = includeBlanksInGreaterThan;
            return this;
        }

        public Builder includeBlanksInRange(boolean includeBlanksInRange) {
            this.includeBlanksInRange = includeBlanksInRange;
            return this;
        }

        public ScalarFilterParams build() {
            return new ScalarFilterParams(this);
        }
    }
}
