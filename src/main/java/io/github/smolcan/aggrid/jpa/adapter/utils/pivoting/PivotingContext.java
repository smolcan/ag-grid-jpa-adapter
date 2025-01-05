package io.github.smolcan.aggrid.jpa.adapter.utils.pivoting;

import io.github.smolcan.aggrid.jpa.adapter.utils.Pair;
import jakarta.persistence.criteria.Selection;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PivotingContext {
    
    private boolean isPivoting;
    private Map<String, List<Object>> pivotValues;
    private List<Set<Pair<String, Object>>> pivotPairs;
    private List<List<Pair<String, Object>>> cartesianProduct;
    private List<Selection<?>> pivotingSelections;
    private List<String> pivotingResultFields;

    public boolean isPivoting() {
        return isPivoting;
    }

    public void setPivoting(boolean pivoting) {
        isPivoting = pivoting;
    }

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

    public List<Selection<?>> getPivotingSelections() {
        return pivotingSelections;
    }

    public void setPivotingSelections(List<Selection<?>> pivotingSelections) {
        this.pivotingSelections = pivotingSelections;
    }
    
    public List<String> getPivotingResultFields() {
        return pivotingResultFields;
    }
    
    public void setPivotingResultFields(List<String> pivotingResultFields) {
        this.pivotingResultFields = pivotingResultFields;
    }
}
