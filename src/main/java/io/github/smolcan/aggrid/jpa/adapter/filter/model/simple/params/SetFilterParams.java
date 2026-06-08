package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import lombok.Builder;
import lombok.Getter;

import java.util.function.BiFunction;

@Getter
@Builder
public class SetFilterParams implements IFilterParams {
    
    private final boolean caseSensitive;
    /**
     * If specified, this formats the text before applying the compare logic, useful for
     * instance to substitute accented characters.
     */
    private final BiFunction<CriteriaBuilder, Expression<String>, Expression<String>> textFormatter;
    
}
