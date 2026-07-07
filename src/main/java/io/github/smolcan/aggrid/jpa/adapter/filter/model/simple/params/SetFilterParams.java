package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import lombok.Builder;
import lombok.Getter;

import java.util.function.BiFunction;

@Getter
@Builder
public class SetFilterParams implements IFilterParams {
    
    /**
     * @param caseSensitive whether matching is case-sensitive (default {@code false}).
     * @return whether matching is case-sensitive.
     */
    private final boolean caseSensitive;
    /**
     * If specified, this formats the text before applying the compare logic, useful for
     * instance to substitute accented characters.
     *
     * @param textFormatter formats the text before comparing.
     * @return the configured text formatter.
     */
    private final BiFunction<CriteriaBuilder, Expression<String>, Expression<String>> textFormatter;
    
}
