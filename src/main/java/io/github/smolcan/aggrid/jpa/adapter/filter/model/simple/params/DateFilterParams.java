package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

public class DateFilterParams extends ScalarFilterParams {

    public DateFilterParams(boolean inRangeInclusive, boolean includeBlanksInEquals, boolean includeBlanksInNotEqual, boolean includeBlanksInLessThan, boolean includeBlanksInGreaterThan, boolean includeBlanksInRange) {
        super(inRangeInclusive, includeBlanksInEquals, includeBlanksInNotEqual, includeBlanksInLessThan, includeBlanksInGreaterThan, includeBlanksInRange);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder extends ScalarFilterParams.Builder {
        
        public DateFilterParams build() {
            return new DateFilterParams(
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
