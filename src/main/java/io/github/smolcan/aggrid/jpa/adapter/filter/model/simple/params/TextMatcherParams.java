package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import jakarta.persistence.criteria.Expression;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class TextMatcherParams {

    /**
     * The applicable filter option being tested.
     *
     * @param filterOption the applicable filter option being tested.
     * @return the applicable filter option being tested.
     */
    @NonNull
    private final SimpleFilterModelType filterOption;

    /**
     * The expression about to be filtered.
     * If a `textFormatter` is provided, this value will have been formatted.
     * If no `textFormatter` is provided and `caseSensitive` is not provided or is `false`,
     * the value will have been converted to lower case.
     *
     * @param value the column expression about to be filtered.
     * @return the column expression about to be filtered.
     */
    @NonNull
    private final Expression<String> value;

    /**
     * The value to filter by.
     * If a `textFormatter` is provided, this value will have been formatted.
     * If no `textFormatter` is provided and `caseSensitive` is not provided or is `false`,
     * the value will have been converted to lower case.
     *
     * @param filterText the text to filter by.
     * @return the text to filter by.
     */
    @NonNull
    private final Expression<String> filterText;
    
}