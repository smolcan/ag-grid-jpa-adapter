package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class ScalarFilterParams implements ISimpleFilterParams {
    /**
     * @param inRangeInclusive if {@code true}, the {@code 'inRange'} option includes the range endpoints.
     * @return whether the {@code 'inRange'} option is inclusive.
     */
    private final boolean inRangeInclusive;
    /**
     * @param includeBlanksInEquals if {@code true}, blank (null) values pass the {@code 'equals'} option.
     * @return whether blanks pass {@code 'equals'}.
     */
    private final boolean includeBlanksInEquals;
    /**
     * @param includeBlanksInNotEqual if {@code true}, blank (null) values pass the {@code 'notEqual'} option.
     * @return whether blanks pass {@code 'notEqual'}.
     */
    private final boolean includeBlanksInNotEqual;
    /**
     * @param includeBlanksInLessThan if {@code true}, blank (null) values pass {@code 'lessThan'}/{@code 'lessThanOrEqual'}.
     * @return whether blanks pass {@code 'lessThan'}.
     */
    private final boolean includeBlanksInLessThan;
    /**
     * @param includeBlanksInGreaterThan if {@code true}, blank (null) values pass {@code 'greaterThan'}/{@code 'greaterThanOrEqual'}.
     * @return whether blanks pass {@code 'greaterThan'}.
     */
    private final boolean includeBlanksInGreaterThan;
    /**
     * @param includeBlanksInRange if {@code true}, blank (null) values pass the {@code 'inRange'} option.
     * @return whether blanks pass {@code 'inRange'}.
     */
    private final boolean includeBlanksInRange;
}
