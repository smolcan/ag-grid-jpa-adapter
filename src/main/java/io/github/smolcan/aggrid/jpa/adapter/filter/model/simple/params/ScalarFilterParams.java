package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
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
}
