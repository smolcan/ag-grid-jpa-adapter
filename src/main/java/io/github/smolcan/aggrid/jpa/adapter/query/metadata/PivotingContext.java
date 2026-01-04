package io.github.smolcan.aggrid.jpa.adapter.query.metadata;

import io.github.smolcan.aggrid.jpa.adapter.utils.Pair;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Selection;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * To hold all the needed information about pivoting
 */
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

    public Map<String, List<Object>> getPivotValues() {
        return pivotValues;
    }

    public void setPivotValues(Map<String, List<Object>> pivotValues) {
        this.pivotValues = pivotValues;
    }

    public List<Set<Pair<String, Object>>> getPivotPairs() {
        return pivotPairs;
    }

    public void setPivotPairs(List<Set<Pair<String, Object>>> pivotPairs) {
        this.pivotPairs = pivotPairs;
    }

    public List<List<Pair<String, Object>>> getCartesianProduct() {
        return cartesianProduct;
    }

    public void setCartesianProduct(List<List<Pair<String, Object>>> cartesianProduct) {
        this.cartesianProduct = cartesianProduct;
    }
    
    public List<String> getPivotingResultFields() {
        return pivotingResultFields;
    }
    
    public void setPivotingResultFields(List<String> pivotingResultFields) {
        this.pivotingResultFields = pivotingResultFields;
    }

    public Map<String, Expression<?>> getColumnNamesToExpression() {
        return columnNamesToExpression;
    }

    public void setColumnNamesToExpression(Map<String, Expression<?>> columnNamesToExpression) {
        this.columnNamesToExpression = columnNamesToExpression;
    }
}
