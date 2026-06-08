package io.github.smolcan.aggrid.jpa.adapter.query.metadata;

import io.github.smolcan.aggrid.jpa.adapter.utils.Pair;
import jakarta.persistence.criteria.Expression;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * To hold all the needed information about pivoting
 */
@Setter
@Getter
public class PivotingContext {
    
    // for each column, its distinct values
    private Map<String, List<Object>> pivotValues;
    
    // each pivot col with pair with its distinct values
    // for example: [
    //      [(book, Book1), (book, Book2)], 
    //      [(product, Product1), (product, Product2)]
    // ]
    private List<Set<Pair<String, Object>>> pivotPairs;
    
    // cartesian product of pivotPairs
    private List<List<Pair<String, Object>>> cartesianProduct;
    
    private Map<String, Expression<?>> columnNamesToExpression;
    
    // pivoting result fields for response
    private List<String> pivotingResultFields;

}
