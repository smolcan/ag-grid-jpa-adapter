package io.github.smolcan.aggrid.jpa.adapter.filter.simple.params;

public class NumberFilterParams extends ScalarFilterParams {
    
    public NumberFilterParams(boolean inRangeInclusive, boolean includeBlanksInEquals, boolean includeBlanksInNotEqual, boolean includeBlanksInLessThan, boolean includeBlanksInGreaterThan, boolean includeBlanksInRange) {
        super(inRangeInclusive, includeBlanksInEquals, includeBlanksInNotEqual, includeBlanksInLessThan, includeBlanksInGreaterThan, includeBlanksInRange);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder extends ScalarFilterParams.Builder {
        
        public NumberFilterParams build() {
            return new NumberFilterParams(
                    inRangeInclusive,
                    includeBlanksInEquals,
                    includeBlanksInNotEqual,
                    includeBlanksInLessThan,
                    includeBlanksInGreaterThan,
                    includeBlanksInRange
            );
        }
        
    }
}
