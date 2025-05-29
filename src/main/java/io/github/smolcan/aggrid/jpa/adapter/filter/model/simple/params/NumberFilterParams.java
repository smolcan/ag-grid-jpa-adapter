package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

public class NumberFilterParams extends ScalarFilterParams {
    
    public NumberFilterParams(Builder builder) {
        super(builder);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder extends ScalarFilterParams.Builder {

        @Override
        public Builder inRangeInclusive(boolean inRangeInclusive) {
            super.inRangeInclusive(inRangeInclusive);
            return this;
        }

        @Override
        public Builder includeBlanksInEquals(boolean includeBlanksInEquals) {
            super.includeBlanksInEquals(includeBlanksInEquals);
            return this;
        }

        @Override
        public Builder includeBlanksInNotEqual(boolean includeBlanksInNotEqual) {
            super.includeBlanksInNotEqual(includeBlanksInNotEqual);
            return this;
        }

        @Override
        public Builder includeBlanksInLessThan(boolean includeBlanksInLessThan) {
            super.includeBlanksInLessThan(includeBlanksInLessThan);
            return this;
        }

        @Override
        public Builder includeBlanksInGreaterThan(boolean includeBlanksInGreaterThan) {
            super.includeBlanksInGreaterThan(includeBlanksInGreaterThan);
            return this;
        }

        @Override
        public Builder includeBlanksInRange(boolean includeBlanksInRange) {
            super.includeBlanksInRange(includeBlanksInRange);
            return this;
        }

        @Override
        public NumberFilterParams build() {
            return new NumberFilterParams(this);
        }
    }
}
